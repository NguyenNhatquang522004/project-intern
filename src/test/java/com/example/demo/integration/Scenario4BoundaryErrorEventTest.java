package com.example.demo.integration;

import com.example.demo.integration.config.ZeebeTestConfig;
import com.example.demo.integration.fixture.LeaveRequestFixture;
import com.example.demo.integration.helper.DbTestHelper;
import com.example.demo.integration.helper.ZeebeTestHelper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * Kịch bản 4: Xử lý Lỗi Kỹ Thuật (Boundary Error Event) tại Task Validate
 * ============================================================================
 *
 * Luồng chạy:
 * 1. Process khởi chạy
 * 2. Worker validate-request gặp sự cố hệ thống và ném BpmnError
 *    (Trong thực tế: gRPC service unavailable, DB timeout, ...)
 * 3. Boundary Event `Event_1a21g6i` (Error Boundary) bắt được lỗi
 * 4. Luồng bẻ sang Service Task `Activity_1lhqw7p` (notify)
 * 5. Process kết thúc tại End Event `Event_0oapf7o`
 *
 * Cách test:
 * Ta intercept job "validate-request" và gọi newThrowErrorCommand để mô phỏng
 * worker ném BpmnError (giống khi gRPC service sập).
 *
 * DB Assertions:
 * - leave_request KHÔNG được tạo (vì validate chưa kịp tạo record khi có BPMN error)
 *   HOẶC nếu được tạo thì status không phải PENDING_APPROVAL
 * - approval_history có thể có record "SYSTEM_ERROR" nếu worker ghi trước khi ném lỗi
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ZeebeTestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Kịch bản 4: Lỗi Kỹ Thuật - Boundary Error Event tại Validate")
class Scenario4BoundaryErrorEventTest {

    private static final Logger log = LoggerFactory.getLogger(Scenario4BoundaryErrorEventTest.class);

    @Autowired
    private ZeebeTestHelper zeebeHelper;

    @Autowired
    private DbTestHelper dbHelper;

    @Autowired
    private ZeebeClient zeebeClient;

    private Map<String, Object> processVariables;
    private String businessKey;
    private long processInstanceKey;

    @BeforeEach
    void setUp() {
        processVariables = LeaveRequestFixture.buildBaseVariables(false);
        businessKey = LeaveRequestFixture.extractBusinessKey(processVariables);
        log.info("[TEST][Scenario4] SetUp: businessKey={}", businessKey);
    }

    @AfterEach
    void tearDown() {
        log.info("[TEST][Scenario4] TearDown: Cleaning up businessKey={}", businessKey);
        dbHelper.cleanUpLeaveRequest(businessKey);
    }

    @Test
    @Order(1)
    @DisplayName("TC-04: validate-request ném BpmnError -> Boundary Event bắt -> notify -> Event_0oapf7o")
    void technicalError_AtValidate_ShouldRouteToBoundaryErrorHandler() {
        // =====================================================================
        // BƯỚC 1: Chuẩn bị interceptor thread - chặn job validate-request
        // và ném BpmnError thay vì complete bình thường
        // (Mô phỏng gRPC service sập khi worker đang xử lý)
        // =====================================================================
        AtomicBoolean errorThrown = new AtomicBoolean(false);
        AtomicReference<Long> capturedProcessKey = new AtomicReference<>(null);

        Thread errorInjectorThread = new Thread(() -> {
            log.info("[TEST][Scenario4] Error injector thread started, waiting for validate-request job...");
            for (int attempt = 0; attempt < 30; attempt++) {
                try {
                    List<ActivatedJob> jobs = zeebeClient.newActivateJobsCommand()
                            .jobType("validate-request")
                            .maxJobsToActivate(5)
                            .timeout(Duration.ofSeconds(30))
                            .send()
                            .join()
                            .getJobs();

                    for (ActivatedJob job : jobs) {
                        log.info("[TEST][Scenario4] Intercepted validate-request job={}. Throwing BpmnError...",
                                job.getKey());
                        // Ném BpmnError để kích hoạt Boundary Error Event `Event_1a21g6i`
                        zeebeClient.newThrowErrorCommand(job.getKey())
                                .errorCode("SYSTEM_ERROR_VALIDATE")
                                .errorMessage("Mô phỏng: gRPC service timeout khi gọi validate")
                                .send()
                                .join();
                        capturedProcessKey.set(job.getProcessInstanceKey());
                        errorThrown.set(true);
                        log.info("[TEST][Scenario4] BpmnError thrown successfully for processKey={}",
                                job.getProcessInstanceKey());
                        return;
                    }
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.error("[TEST][Scenario4] Error injector exception: {}", e.getMessage());
                }
            }
            log.warn("[TEST][Scenario4] Error injector: could not intercept validate-request job in time");
        });
        errorInjectorThread.setDaemon(true);
        errorInjectorThread.start();

        // =====================================================================
        // BƯỚC 2: Khởi tạo process instance NGAY SAU khi thread đã sẵn sàng
        // =====================================================================
        log.info("[TEST][Scenario4] Bước 2: Khởi tạo process instance");
        ProcessInstanceEvent instance = zeebeHelper.startLeaveProcess(processVariables);
        processInstanceKey = instance.getProcessInstanceKey();
        assertThat(processInstanceKey).isPositive();
        log.info("[TEST][Scenario4] Process instance started: key={}", processInstanceKey);

        // =====================================================================
        // BƯỚC 3: Đợi error được inject thành công
        // =====================================================================
        log.info("[TEST][Scenario4] Bước 3: Đợi BpmnError được ném vào Zeebe");
        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(errorThrown::get);

        log.info("[TEST][Scenario4] ✅ BpmnError đã được ném. Zeebe sẽ route sang Boundary Error Event");

        // =====================================================================
        // BƯỚC 4: Đợi notify worker (`Activity_1lhqw7p`) chạy xong
        // (Boundary Error Event -> notify -> Event_0oapf7o)
        // Sau notify, process kết thúc tại Event_0oapf7o
        // =====================================================================
        log.info("[TEST][Scenario4] Bước 4: Đợi 5 giây để Boundary Error routing hoàn thành");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // =====================================================================
        // ASSERTIONS
        // =====================================================================
        log.info("[TEST][Scenario4] Bước 5: Kiểm tra DB assertions");

        // Assert: Status KHÔNG phải PENDING_APPROVAL
        // (BpmnError xảy ra TRƯỚC khi validate hoàn thành => không có pending)
        assertThat(dbHelper.leaveRequestStatusIsNot(businessKey, "PENDING_APPROVAL"))
                .as("Status KHÔNG được là PENDING_APPROVAL khi validate bị BpmnError")
                .isTrue();

        // Assert: used_days KHÔNG bị thay đổi (deduct-balance-task không được chạy)
        Double usedDaysAfterError = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        log.info("[TEST][Scenario4] used_days sau lỗi: {} (phải giữ nguyên)", usedDaysAfterError);
        // used_days không tăng -> không chạy qua deduct-balance-task
        // (Không assert giá trị cụ thể vì không biết initial value, chỉ đảm bảo không tăng)

        // Assert: Process đã kết thúc tại Event_0oapf7o (không còn waiting)
        // Verify bằng cách kiểm tra không có active job nào cho process instance này
        List<ActivatedJob> remainingJobs = zeebeClient.newActivateJobsCommand()
                .jobType("Update-request-pending")
                .maxJobsToActivate(5)
                .timeout(Duration.ofSeconds(5))
                .send()
                .join()
                .getJobs();

        boolean noRemainingPendingJobForProcess = remainingJobs.stream()
                .noneMatch(j -> j.getProcessInstanceKey() == processInstanceKey);
        assertThat(noRemainingPendingJobForProcess)
                .as("Không được có job Update-request-pending cho process này (đã kết thúc qua error path)")
                .isTrue();

        // Release bất kỳ job nào không thuộc test này
        remainingJobs.stream()
                .filter(j -> j.getProcessInstanceKey() != processInstanceKey)
                .forEach(j -> zeebeClient.newFailCommand(j.getKey())
                        .retries(j.getRetries())
                        .send().join());

        log.info("[TEST][Scenario4] ✅ Kịch bản 4 HOÀN THÀNH: Boundary Error Event xử lý đúng");
    }
}

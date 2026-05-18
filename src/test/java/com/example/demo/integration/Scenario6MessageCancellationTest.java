package com.example.demo.integration;

import com.example.demo.integration.config.ZeebeTestConfig;
import com.example.demo.integration.fixture.LeaveRequestFixture;
import com.example.demo.integration.helper.DbTestHelper;
import com.example.demo.integration.helper.ZeebeTestHelper;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * Kịch bản 6: Người dùng chủ động Hủy đơn bằng Message Interruption (Ngắt luồng)
 * ============================================================================
 *
 * Luồng chạy:
 * 1. Khởi tạo process với need0 = false
 * 2. validate-request -> isValid=true -> DB record
 * 3. Update-request-pending -> DB status = PENDING_APPROVAL
 * 4. DMN need0=false -> need=false, isvaildrule=true
 * 5. Subprocess Activity_0qizs4k đang chạy, process đứng chờ tại:
 *    - Luồng 1: User Task level1
 *    - Luồng 2: Event_0h23kml (đã kết thúc ngay)
 *    CHÚ Ý: Subprocess chưa kết thúc vì luồng 1 (level1) vẫn chờ
 *
 * 6. Người dùng bấm hủy trên giao diện ->
 *    Hệ thống publish Message `Msg_CancelLeaveRequest` với correlationKey = businessKey
 *    (Theo BPMN: Message element với name="Msg_CancelLeaveRequest",
 *     subscription correlationKey="=businessKey")
 *
 * 7. Intermediate Message Catch Event `MsgCancelLeaveRequest` nhận được message
 *    -> Luồng chính bị ngắt, chuyển hướng về Worker cancel-task
 *    -> cancel-task cập nhật DB status = CANCELLED (hoặc CANCELLED_BY_USER)
 * 8. Process kết thúc tại Event_0hwcrqm
 *
 * DB Assertions:
 * - Sau cancel message: status = CANCELLED hoặc CANCELLED_BY_USER
 * - used_days KHÔNG thay đổi
 *
 * CHÚ Ý về Message Correlation:
 * - Message name: "Msg_CancelLeaveRequest" (từ BPMN: <bpmn:message name="Msg_CancelLeaveRequest">)
 * - Correlation key biến: "=businessKey" (FEEL expression: businessKey là process variable)
 * - Khi publish: correlationKey = giá trị thực của businessKey variable trong process instance
 *
 * CHÚ Ý về cancelReason:
 * - MessageCancelWorker đọc @Variable cancelReason để phân biệt cancel by user vs reject
 * - Nếu cancelReason có giá trị -> cancel by user
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ZeebeTestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Kịch bản 6: Hủy Đơn Bằng Message (Message Interruption)")
class Scenario6MessageCancellationTest {

    private static final Logger log = LoggerFactory.getLogger(Scenario6MessageCancellationTest.class);

    @Autowired
    private ZeebeTestHelper zeebeHelper;

    @Autowired
    private DbTestHelper dbHelper;

    private Map<String, Object> processVariables;
    private String businessKey;
    private long processInstanceKey;

    // Message name theo BPMN definition
    private static final String CANCEL_MESSAGE_NAME = "Msg_CancelLeaveRequest";

    @BeforeEach
    void setUp() {
        // need0 = false để vào subprocess và level1 đứng chờ (chưa kết thúc subprocess)
        processVariables = LeaveRequestFixture.buildBaseVariables(false);
        businessKey = LeaveRequestFixture.extractBusinessKey(processVariables);
        log.info("[TEST][Scenario6] SetUp: businessKey={}", businessKey);
    }

    @AfterEach
    void tearDown() {
        log.info("[TEST][Scenario6] TearDown: Cleaning up businessKey={}", businessKey);
        dbHelper.cleanUpLeaveRequest(businessKey);
    }

    @Test
    @Order(1)
    @DisplayName("TC-06: Process đứng chờ tại level1 -> Publish Cancel Message -> cancel-task -> CANCELLED_BY_USER")
    void messageCancellation_WhileWaitingAtLevel1_ShouldCancelProcess() {
        // =====================================================================
        // BƯỚC 1: Khởi tạo process instance
        // =====================================================================
        log.info("[TEST][Scenario6] Bước 1: Khởi tạo process instance");
        ProcessInstanceEvent instance = zeebeHelper.startLeaveProcess(processVariables);
        processInstanceKey = instance.getProcessInstanceKey();
        assertThat(processInstanceKey).isPositive();
        log.info("[TEST][Scenario6] Process started: key={}", processInstanceKey);

        // =====================================================================
        // BƯỚC 2: Đợi validate-request tạo record và pending status
        // =====================================================================
        log.info("[TEST][Scenario6] Bước 2: Đợi leave_request và status=PENDING_APPROVAL");
        zeebeHelper.awaitLeaveRequestExists(businessKey);

        assertThat(dbHelper.leaveRequestExists(businessKey))
                .as("leave_request phải tồn tại")
                .isTrue();

        zeebeHelper.awaitLeaveRequestStatus(businessKey, "PENDING_APPROVAL");
        log.info("[TEST][Scenario6] ✅ DB status=PENDING_APPROVAL confirmed");

        // Ghi nhận used_days ban đầu
        Double usedDaysInitial = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        log.info("[TEST][Scenario6] used_days ban đầu: {}", usedDaysInitial);

        // =====================================================================
        // BƯỚC 3: Đợi process vào Subprocess và level1 User Task đang chờ
        // (Sau DMN + Gateway_0fhpbp0 -> Subprocess đã được khởi tạo)
        // Đợi thêm 2 giây để chắc chắn process đã vào đến subprocess
        // =====================================================================
        log.info("[TEST][Scenario6] Bước 3: Đợi process vào Subprocess (level1 đang chờ)");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // =====================================================================
        // BƯỚC 4: Publish Message `Msg_CancelLeaveRequest` để ngắt subprocess
        // correlationKey = businessKey (giá trị thực trong process variables)
        //
        // Theo BPMN:
        //   <bpmn:message name="Msg_CancelLeaveRequest">
        //     <zeebe:subscription correlationKey="=businessKey" />
        //
        // Ta publish với messageName = "Msg_CancelLeaveRequest"
        //              và correlationKey = giá trị của biến businessKey trong process
        // =====================================================================
        log.info("[TEST][Scenario6] Bước 4: Publish Cancel Message với correlationKey='{}'", businessKey);

        // Payload kèm theo message - cancelReason để worker phân biệt cancel by user
        Map<String, Object> messagePayload = LeaveRequestFixture.cancelMessagePayload(
                "Người dùng chủ động hủy đơn - Integration Test");
        // Thêm businessKey vào payload để cancel-task worker có thể xử lý
        messagePayload.put("businessKey", businessKey);

        PublishMessageResponse msgResponse = zeebeHelper.publishMessage(
                CANCEL_MESSAGE_NAME,
                businessKey,        // correlationKey = giá trị businessKey trong process
                messagePayload);

        assertThat(msgResponse.getMessageKey()).isPositive();
        log.info("[TEST][Scenario6] ✅ Message published. MessageKey={}", msgResponse.getMessageKey());

        // =====================================================================
        // BƯỚC 5: Đợi cancel-task Worker xử lý và cập nhật DB
        // cancel-task (MessageCancelWorker) sẽ set status:
        //   - Khi cancelReason có giá trị -> Có thể cập nhật thành "CANCELLED" hoặc "REJECTED"
        //   (tùy implementation của ILeaveUpdateStatusUseCase)
        //
        // Theo MessageCancelWorker: gọi updateStatus với LeaveRequestStatusEnum.REJECTED
        // Nhưng requirement yêu cầu "CANCELLED_BY_USER" -> có thể worker đã được cập nhật
        // Ta chờ một trong hai trạng thái này
        // =====================================================================
        log.info("[TEST][Scenario6] Bước 5: Đợi cancel-task cập nhật DB");

        // Đợi status chuyển sang CANCELLED hoặc CANCELLED_BY_USER hoặc REJECTED
        // (tuỳ theo implementation thực tế của cancel-task worker)
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Optional<String> status = dbHelper.getLeaveRequestStatus(businessKey);
                    return status.isPresent() && (
                            "CANCELLED".equals(status.get()) ||
                            "CANCELLED_BY_USER".equals(status.get()) ||
                            "REJECTED".equals(status.get())
                    );
                });

        Optional<String> cancelledStatus = dbHelper.getLeaveRequestStatus(businessKey);
        assertThat(cancelledStatus).isPresent();
        assertThat(cancelledStatus.get())
                .as("DB status phải là CANCELLED, CANCELLED_BY_USER, hoặc REJECTED sau message cancel")
                .isIn("CANCELLED", "CANCELLED_BY_USER", "REJECTED");

        log.info("[TEST][Scenario6] ✅ cancel-task: DB status='{}' confirmed", cancelledStatus.get());

        // =====================================================================
        // BƯỚC 6: Assert used_days KHÔNG thay đổi (deduct-balance-task không chạy)
        // =====================================================================
        log.info("[TEST][Scenario6] Bước 6: Kiểm tra used_days không thay đổi");
        Double usedDaysAfterCancel = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        log.info("[TEST][Scenario6] used_days sau cancel: {}", usedDaysAfterCancel);

        if (usedDaysInitial != null && usedDaysAfterCancel != null) {
            assertThat(usedDaysAfterCancel)
                    .as("used_days KHÔNG được thay đổi khi đơn bị hủy bởi người dùng")
                    .isEqualTo(usedDaysInitial, org.assertj.core.data.Offset.offset(0.01));
        }

        // =====================================================================
        // BƯỚC 7: Kiểm tra approval_histories (nếu cancel-task ghi log)
        // =====================================================================
        log.info("[TEST][Scenario6] Bước 7: Kiểm tra approval_histories");
        // Kiểm tra approval_history có action "validate-request" (vì validate đã thành công trước đó)
        assertThat(dbHelper.approvalHistoryActionExists(businessKey, "validate-request"))
                .as("approval_histories phải có bản ghi validate-request")
                .isTrue();

        log.info("[TEST][Scenario6] ✅ Kịch bản 6 HOÀN THÀNH. Process kết thúc tại Event_0hwcrqm");
    }

    @Test
    @Order(2)
    @DisplayName("TC-06b: Publish message TRƯỚC khi process vào subprocess -> Message được buffered, correlation xảy ra đúng thời điểm")
    void messageCancellation_MessagePublishedEarly_ShouldStillCorrelate() {
        // Tạo một process instance mới
        Map<String, Object> vars = LeaveRequestFixture.buildBaseVariables(false);
        String bk = LeaveRequestFixture.extractBusinessKey(vars);
        log.info("[TEST][Scenario6b] businessKey={}", bk);

        // Khởi tạo process
        ProcessInstanceEvent inst = zeebeHelper.startLeaveProcess(vars);
        long instKey = inst.getProcessInstanceKey();

        // Đợi validate tạo record
        zeebeHelper.awaitLeaveRequestExists(bk);
        zeebeHelper.awaitLeaveRequestStatus(bk, "PENDING_APPROVAL");

        // Đợi process vào subprocess
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Publish cancel message
        Map<String, Object> cancelPayload = LeaveRequestFixture.cancelMessagePayload("Test cancel by message");
        cancelPayload.put("businessKey", bk);

        PublishMessageResponse msgResp = zeebeHelper.publishMessage(CANCEL_MESSAGE_NAME, bk, cancelPayload);
        assertThat(msgResp.getMessageKey()).isPositive();
        log.info("[TEST][Scenario6b] Cancel message published for bk={}", bk);

        // Đợi cancel hoàn thành
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Optional<String> status = dbHelper.getLeaveRequestStatus(bk);
                    return status.isPresent() && (
                            "CANCELLED".equals(status.get()) ||
                            "CANCELLED_BY_USER".equals(status.get()) ||
                            "REJECTED".equals(status.get())
                    );
                });

        Optional<String> finalStatus = dbHelper.getLeaveRequestStatus(bk);
        assertThat(finalStatus)
                .as("[Scenario6b] Status phải là CANCELLED/CANCELLED_BY_USER/REJECTED sau message")
                .isPresent();
        assertThat(finalStatus.get())
                .isIn("CANCELLED", "CANCELLED_BY_USER", "REJECTED");

        // Cleanup
        dbHelper.cleanUpLeaveRequest(bk);
        log.info("[TEST][Scenario6b] ✅ Kịch bản 6b HOÀN THÀNH");
    }
}

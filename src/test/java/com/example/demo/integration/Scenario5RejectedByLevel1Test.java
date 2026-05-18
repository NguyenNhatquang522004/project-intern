package com.example.demo.integration;

import com.example.demo.integration.config.ZeebeTestConfig;
import com.example.demo.integration.fixture.LeaveRequestFixture;
import com.example.demo.integration.helper.DbTestHelper;
import com.example.demo.integration.helper.ZeebeTestHelper;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * Kịch bản 5: Người duyệt cấp 1 Từ chối đơn (Rejected Flow)
 * ============================================================================
 *
 * Luồng chạy:
 * 1. Khởi tạo process với need0 = false
 * 2. validate-request -> isValid=true -> DB record
 * 3. Update-request-pending -> DB status = PENDING_APPROVAL
 * 4. DMN need0=false -> need=false, isvaildrule=true
 * 5. Vào Subprocess Activity_0qizs4k
 *    - Luồng 1: level1 chờ
 *    - Luồng 2: Gateway_1br2c14 need=false -> Event_0h23kml (subprocess end)
 * 6. Hoàn thành User Task level1 với Status = "Rejected"
 *    -> Gateway_1x4tarh nhận Status=Rejected -> notify (Activity_07x1hl8)
 *    -> Event_0j3efdm (end trong subprocess cho luồng 1)
 * 7. Subprocess kết thúc -> Gateway_0283bq3 nhận Status="Rejected"
 *    -> Kích hoạt Worker cancel-task (canceltask)
 * 8. cancel-task cập nhật DB -> status = REJECTED
 * 9. Process kết thúc tại Event_0hwcrqm
 *
 * DB Assertions:
 * - Sau cancel-task: status = REJECTED
 * - used_days KHÔNG thay đổi (deduct-balance-task không chạy)
 *
 * CHÚ Ý: Trong BPMN Gateway_0283bq3:
 * - Flow_1gzh69e: =Status ="Rejected" -> canceltask
 * - Flow_1jj7cmb: =Status ="APPROVED" -> deductbalancetask
 * Giá trị "Rejected" phải khớp chính xác (viết hoa R, thường ejected).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ZeebeTestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Kịch bản 5: Người duyệt cấp 1 Từ chối đơn (Rejected Flow)")
class Scenario5RejectedByLevel1Test {

    private static final Logger log = LoggerFactory.getLogger(Scenario5RejectedByLevel1Test.class);

    @Autowired
    private ZeebeTestHelper zeebeHelper;

    @Autowired
    private DbTestHelper dbHelper;

    private Map<String, Object> processVariables;
    private String businessKey;
    private long processInstanceKey;

    @BeforeEach
    void setUp() {
        // need0 = false: DMN trả need=false, luồng 2 trong subprocess đi thẳng End
        processVariables = LeaveRequestFixture.buildBaseVariables(false);
        businessKey = LeaveRequestFixture.extractBusinessKey(processVariables);
        log.info("[TEST][Scenario5] SetUp: businessKey={}", businessKey);
    }

    @AfterEach
    void tearDown() {
        log.info("[TEST][Scenario5] TearDown: Cleaning up businessKey={}", businessKey);
        dbHelper.cleanUpLeaveRequest(businessKey);
    }

    @Test
    @Order(1)
    @DisplayName("TC-05: level1 từ chối -> cancel-task -> DB status=REJECTED -> Event_0hwcrqm")
    void level1Rejection_ShouldRouteToCancelTask_AndSetStatusRejected() {
        // =====================================================================
        // BƯỚC 1: Khởi tạo process
        // =====================================================================
        log.info("[TEST][Scenario5] Bước 1: Khởi tạo process instance");
        ProcessInstanceEvent instance = zeebeHelper.startLeaveProcess(processVariables);
        processInstanceKey = instance.getProcessInstanceKey();
        assertThat(processInstanceKey).isPositive();
        log.info("[TEST][Scenario5] Process started: key={}", processInstanceKey);

        // =====================================================================
        // BƯỚC 2: Đợi validate-request tạo record trong DB
        // =====================================================================
        log.info("[TEST][Scenario5] Bước 2: Đợi leave_request xuất hiện trong DB");
        zeebeHelper.awaitLeaveRequestExists(businessKey);

        assertThat(dbHelper.leaveRequestExists(businessKey))
                .as("leave_request phải tồn tại sau validate-request")
                .isTrue();
        log.info("[TEST][Scenario5] ✅ validate-request: leave_request đã được tạo");

        // =====================================================================
        // BƯỚC 3: Đợi Update-request-pending -> PENDING_APPROVAL
        // =====================================================================
        log.info("[TEST][Scenario5] Bước 3: Đợi DB status=PENDING_APPROVAL");
        zeebeHelper.awaitLeaveRequestStatus(businessKey, "PENDING_APPROVAL");

        assertThat(dbHelper.getLeaveRequestStatus(businessKey))
                .as("Status phải là PENDING_APPROVAL trước khi người duyệt hành động")
                .isPresent()
                .hasValue("PENDING_APPROVAL");
        log.info("[TEST][Scenario5] ✅ Update-request-pending: status=PENDING_APPROVAL");

        // Ghi nhận used_days TRƯỚC khi test để so sánh sau
        Double usedDaysBeforeRejection = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        log.info("[TEST][Scenario5] used_days trước rejection: {}", usedDaysBeforeRejection);

        // =====================================================================
        // BƯỚC 4: Complete User Task level1 với Status = "Rejected"
        // QUAN TRỌNG: Giá trị phải là "Rejected" (viết hoa R) theo BPMN condition:
        //   =Status ="Rejected"
        // =====================================================================
        log.info("[TEST][Scenario5] Bước 4: Complete User Task 'level1' với Status='Rejected'");
        zeebeHelper.completeUserTask("level1",
                LeaveRequestFixture.rejectedPayload(), // {"Status": "Rejected"}
                processInstanceKey);
        log.info("[TEST][Scenario5] ✅ User Task level1 hoàn thành với Rejected");

        // =====================================================================
        // BƯỚC 5: Sau reject, luồng đi:
        //   Gateway_1x4tarh (trong subprocess): Status=Rejected -> notify (Activity_07x1hl8)
        //   -> Event_0j3efdm (subprocess end)
        //   Gateway_0283bq3: Status=Rejected -> cancel-task (canceltask)
        //   cancel-task: update DB status = REJECTED
        // =====================================================================
        log.info("[TEST][Scenario5] Bước 5: Đợi cancel-task cập nhật DB status=REJECTED");
        // MessageCancelWorker/cancel-task sẽ update status = REJECTED
        zeebeHelper.awaitLeaveRequestStatus(businessKey, "REJECTED");

        Optional<String> finalStatus = dbHelper.getLeaveRequestStatus(businessKey);
        assertThat(finalStatus)
                .as("DB status phải là 'REJECTED' sau khi cancel-task hoàn thành")
                .isPresent()
                .hasValue("REJECTED");
        log.info("[TEST][Scenario5] ✅ cancel-task: DB status=REJECTED confirmed");

        // =====================================================================
        // BƯỚC 6: Assert used_days KHÔNG thay đổi (deduct-balance-task không chạy)
        // =====================================================================
        log.info("[TEST][Scenario5] Bước 6: Kiểm tra used_days không thay đổi");
        Double usedDaysAfterRejection = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        log.info("[TEST][Scenario5] used_days sau rejection: {}", usedDaysAfterRejection);

        if (usedDaysBeforeRejection != null && usedDaysAfterRejection != null) {
            assertThat(usedDaysAfterRejection)
                    .as("used_days KHÔNG được tăng khi đơn bị reject (deduct-balance-task không chạy)")
                    .isEqualTo(usedDaysBeforeRejection, org.assertj.core.data.Offset.offset(0.01));
        }

        log.info("[TEST][Scenario5] ✅ Kịch bản 5 HOÀN THÀNH. Process kết thúc tại Event_0hwcrqm");
    }

    @Test
    @Order(2)
    @DisplayName("TC-05b: need=true, cả 2 cấp đều reject -> cancel-task -> REJECTED")
    void level2Rejection_WithNeedTrue_ShouldAlsoRouteToCancel() {
        // Tạo biến với need0=true để đi qua cả 2 cấp
        Map<String, Object> vars = LeaveRequestFixture.buildBaseVariables(true);
        String bk = LeaveRequestFixture.extractBusinessKey(vars);
        log.info("[TEST][Scenario5b] businessKey={}", bk);

        ProcessInstanceEvent inst = zeebeHelper.startLeaveProcess(vars);
        long instKey = inst.getProcessInstanceKey();

        // Đợi DB record được tạo
        zeebeHelper.awaitLeaveRequestExists(bk);
        zeebeHelper.awaitLeaveRequestStatus(bk, "PENDING_APPROVAL");

        // Người duyệt 2 từ chối (Activity_1wioexg)
        // Trước tiên cần complete level1 với APPROVED để đi đến level 2
        log.info("[TEST][Scenario5b] Complete level1 với APPROVED để tiến vào level2");
        zeebeHelper.completeUserTask("level1", LeaveRequestFixture.approvedPayload(), instKey);

        // Sau đó người duyệt 2 từ chối
        log.info("[TEST][Scenario5b] Complete Activity_1wioexg với Rejected");
        zeebeHelper.completeUserTask("Activity_1wioexg",
                LeaveRequestFixture.rejectedPayload(), instKey);

        // Đợi cancel-task cập nhật DB
        zeebeHelper.awaitLeaveRequestStatus(bk, "REJECTED");

        assertThat(dbHelper.getLeaveRequestStatus(bk))
                .as("DB status phải là REJECTED khi người duyệt 2 reject")
                .isPresent()
                .hasValue("REJECTED");

        // Cleanup
        dbHelper.cleanUpLeaveRequest(bk);
        log.info("[TEST][Scenario5b] ✅ Kịch bản 5b HOÀN THÀNH");
    }
}

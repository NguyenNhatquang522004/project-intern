package com.example.demo.integration.helper;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * ZeebeTestHelper: Lớp tiện ích tổng hợp cho tất cả integration tests.
 * Cung cấp:
 * - Deploy process/decision
 * - Khởi tạo process instance
 * - Hoàn thành User Task thông qua ZeebeClient REST (Camunda 8.5+)
 * - Publish message (Message Correlation)
 * - Hàm polling/awaitility để đợi trạng thái DB thay đổi
 */
@Component
public class ZeebeTestHelper {

    private static final Logger log = LoggerFactory.getLogger(ZeebeTestHelper.class);

    private final ZeebeClient zeebeClient;
    private final JdbcTemplate jdbcTemplate;

    // Thời gian tối đa Awaitility đợi DB cập nhật
    private static final int AWAIT_TIMEOUT_SECONDS = 10;
    private static final int POLL_INTERVAL_MS = 500;

    public ZeebeTestHelper(ZeebeClient zeebeClient, JdbcTemplate jdbcTemplate) {
        this.zeebeClient = zeebeClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================================
    // 1. DEPLOYMENT HELPERS
    // =========================================================================

    /**
     * Deploy BPMN + DMN resources từ classpath.
     */
    public DeploymentEvent deployResources(String... resourcePaths) {
        if (resourcePaths == null || resourcePaths.length == 0) {
            throw new IllegalArgumentException("[TEST] Danh sách resource paths không được để trống.");
        }
        
        log.info("[TEST] Deploying resources: {}", (Object) resourcePaths);
        
        // Bước 1: Khởi tạo command (Trả về kiểu DeployResourceCommandStep1)
        var step1 = zeebeClient.newDeployResourceCommand();
        
        // Bước 2: Thêm file đầu tiên để chuyển đổi kiểu dữ liệu sang DeployResourceCommandStep2
        var step2 = step1.addResourceFromClasspath(resourcePaths[0]);
        
        // Bước 3: Duyệt các file còn lại nếu có (lúc này kiểu dữ liệu đã là Step2 hợp lệ)
        for (int i = 1; i < resourcePaths.length; i++) {
            step2 = step2.addResourceFromClasspath(resourcePaths[i]);
        }
        
        // Bước 4: Lúc này compiler đã nhận diện được hàm .send() chuẩn xác
        DeploymentEvent event = step2.send().join();
        log.info("[TEST] Deployed successfully. Deployment key: {}", event.getKey());
        return event;
    }

    // =========================================================================
    // 2. PROCESS INSTANCE HELPERS
    // =========================================================================

    /**
     * Khởi tạo một process instance với biến đầu vào cho process `Process_1oveniu`.
     *
     * @param variables Map<String, Object> các biến nghiệp vụ ban đầu
     * @return ProcessInstanceEvent
     */
    public ProcessInstanceEvent startLeaveProcess(Map<String, Object> variables) {
        log.info("[TEST] Starting process 'Process_1oveniu' with variables: {}", variables);
        ProcessInstanceEvent event = zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("Process_1oveniu")
                .latestVersion()
                .variables(variables)
                .send()
                .join();
        log.info("[TEST] Process instance started. Key: {}", event.getProcessInstanceKey());
        return event;
    }

    // =========================================================================
    // 3. USER TASK HELPERS (dùng Zeebe JobWorker để complete user task)
    // =========================================================================

    /**
     * Tìm và hoàn thành User Task theo taskDefinitionId (element id trong BPMN).
     * Sử dụng Zeebe Job Worker API: activate + complete.
     *
     * @param jobType       Job type của task listener "completing" hoặc element id
     * @param userTaskId    Element ID trong BPMN (e.g. "level1")
     * @param outputVars    Biến đầu ra khi complete task (e.g. {"Status": "APPROVED"})
     * @param processKey    Process instance key để lọc đúng task
     */
    public void completeUserTask(String userTaskId, Map<String, Object> outputVars, long processKey) {
        log.info("[TEST] Attempting to complete UserTask '{}' for processKey={}", userTaskId, processKey);

        AtomicReference<Long> jobKeyRef = new AtomicReference<>(null);

        // Awaitility đợi job xuất hiện (user task listener type = "task-complete" hoặc element type)
        // Ta poll bằng activateJobs trực tiếp cho loại job tương ứng
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                    // User Task trong Zeebe 8.5+ tạo ra một zeebe:userTask job có type = element id
                    // Tuy nhiên, bộ test này dùng Zeebe Job Worker API để activate "io.camunda.zeebe:userTask"
                    var jobs = zeebeClient.newActivateJobsCommand()
                            .jobType("io.camunda.zeebe:userTask")
                            .maxJobsToActivate(10)
                            .timeout(Duration.ofSeconds(30))
                            .send()
                            .join()
                            .getJobs();

                    for (var job : jobs) {
                        // Lọc theo processInstanceKey VÀ elementId (BPMN id của User Task)
                        if (job.getProcessInstanceKey() == processKey
                                && userTaskId.equals(job.getElementId())) {
                            jobKeyRef.set(job.getKey());
                            return true;
                        } else {
                            // Không phải job ta cần, trả lại (release)
                            zeebeClient.newFailCommand(job.getKey())
                                    .retries(job.getRetries())
                                    .send()
                                    .join();
                        }
                    }
                    return false;
                });

        long jobKey = jobKeyRef.get();
        log.info("[TEST] Found UserTask job. Key: {}. Completing with vars: {}", jobKey, outputVars);
        zeebeClient.newCompleteCommand(jobKey)
                .variables(outputVars)
                .send()
                .join();
        log.info("[TEST] UserTask '{}' completed successfully.", userTaskId);
    }

    // =========================================================================
    // 4. MESSAGE HELPERS
    // =========================================================================

    /**
     * Publish message để trigger Message Intermediate Catch Event / Boundary Event.
     *
     * @param messageName    Tên message (e.g. "Msg_CancelLeaveRequest")
     * @param correlationKey Correlation key value (e.g. businessKey của đơn)
     * @param variables      Biến kèm theo message
     */
    public PublishMessageResponse publishMessage(String messageName, String correlationKey,
            Map<String, Object> variables) {
        log.info("[TEST] Publishing message '{}' with correlationKey='{}', vars={}",
                messageName, correlationKey, variables);
        PublishMessageResponse response = zeebeClient.newPublishMessageCommand()
                .messageName(messageName)
                .correlationKey(correlationKey)
                .variables(variables)
                .timeToLive(Duration.ofSeconds(30))
                .send()
                .join();
        log.info("[TEST] Message published. MessageKey: {}", response.getMessageKey());
        return response;
    }

    // =========================================================================
    // 5. DATABASE AWAITILITY HELPERS
    // =========================================================================

    /**
     * Đợi (polling) cho đến khi trạng thái leave_request thay đổi thành giá trị mong muốn.
     * Timeout tối đa: AWAIT_TIMEOUT_SECONDS giây.
     *
     * @param businessKey  Business key của đơn phép
     * @param expectedStatus Trạng thái mong muốn (e.g. "PENDING_APPROVAL", "CANCELLED")
     */
    public void awaitLeaveRequestStatus(String businessKey, String expectedStatus) {
        log.info("[TEST] Awaiting DB status='{}' for businessKey='{}'", expectedStatus, businessKey);
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<String> result = jdbcTemplate.queryForList(
                            "SELECT status FROM leave_requests WHERE business_key = ?",
                            String.class, businessKey);
                    return !result.isEmpty() && expectedStatus.equals(result.get(0));
                });
        log.info("[TEST] DB status confirmed: '{}' for businessKey='{}'", expectedStatus, businessKey);
    }

    /**
     * Đợi cho đến khi bảng approval_histories có bản ghi với action tương ứng.
     *
     * @param businessKey Xác định leave request
     * @param action      Action cần kiểm tra (e.g. "validate-request", "approval", "reject")
     */
    public void awaitApprovalHistoryAction(String businessKey, String action) {
        log.info("[TEST] Awaiting approval_histories action='{}' for businessKey='{}'", action, businessKey);
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<String> result = jdbcTemplate.queryForList(
                            "SELECT action FROM approval_histories WHERE leave_request_id::text = "
                                    + "(SELECT id::text FROM leave_requests WHERE business_key = ?) "
                                    + "AND action = ?",
                            String.class, businessKey, action);
                    return !result.isEmpty();
                });
        log.info("[TEST] approval_histories action='{}' confirmed for businessKey='{}'", action, businessKey);
    }

    /**
     * Đọc used_days hiện tại của nhân viên từ bảng leave_balances.
     *
     * @param employeeEmail Email của nhân viên (để join qua employees table)
     * @param leaveTypeCode Code của loại phép (e.g. "ANNUAL")
     * @param year          Năm cần kiểm tra
     * @return used_days hiện tại (dạng Double) hoặc null nếu không tìm thấy
     */
    public Double getLeaveBalanceUsedDays(String employeeEmail, String leaveTypeCode, int year) {
        List<Double> result = jdbcTemplate.queryForList(
                "SELECT lb.used_days " +
                        "FROM leave_balances lb " +
                        "JOIN employees e ON e.id = lb.employee_id " +
                        "JOIN leave_types lt ON lt.id = lb.leave_type_id " +
                        "WHERE e.email = ? AND lt.code = ? AND lb.year = ?",
                Double.class, employeeEmail, leaveTypeCode, year);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Đợi used_days thay đổi đến giá trị mong đợi sau khi deduct-balance-task hoàn thành.
     */
    public void awaitLeaveBalanceUsedDays(String employeeEmail, String leaveTypeCode,
            int year, double expectedUsedDays) {
        log.info("[TEST] Awaiting used_days={} for employee='{}', leaveType='{}', year={}",
                expectedUsedDays, employeeEmail, leaveTypeCode, year);
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Double currentUsedDays = getLeaveBalanceUsedDays(employeeEmail, leaveTypeCode, year);
                    return currentUsedDays != null
                            && Math.abs(currentUsedDays - expectedUsedDays) < 0.01;
                });
        log.info("[TEST] used_days='{}' confirmed for employee='{}'", expectedUsedDays, employeeEmail);
    }

    /**
     * Lấy trạng thái hiện tại của leave_request từ DB.
     */
    public Optional<String> getLeaveRequestStatus(String businessKey) {
        List<String> result = jdbcTemplate.queryForList(
                "SELECT status FROM leave_requests WHERE business_key = ?",
                String.class, businessKey);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Kiểm tra leave_request tồn tại trong DB.
     */
    public boolean leaveRequestExists(String businessKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leave_requests WHERE business_key = ?",
                Integer.class, businessKey);
        return count != null && count > 0;
    }

    /**
     * Awaitility đợi leave_request xuất hiện trong DB (sau validate-request worker).
     */
    public void awaitLeaveRequestExists(String businessKey) {
        log.info("[TEST] Awaiting leave_request to exist in DB for businessKey='{}'", businessKey);
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> leaveRequestExists(businessKey));
        log.info("[TEST] leave_request confirmed in DB for businessKey='{}'", businessKey);
    }

    /**
     * Đợi process instance kết thúc (không còn active trong Operate).
     * Thay thế bằng cách poll DB: khi status = APPROVED/REJECTED/CANCELLED
     * thì process coi như đã kết thúc.
     */
    public void awaitLeaveRequestFinalStatus(String businessKey, String... acceptableStatuses) {
        List<String> acceptable = List.of(acceptableStatuses);
        log.info("[TEST] Awaiting final DB status (one of {}) for businessKey='{}'",
                acceptable, businessKey);
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Optional<String> status = getLeaveRequestStatus(businessKey);
                    return status.isPresent() && acceptable.contains(status.get());
                });
        log.info("[TEST] Final status confirmed for businessKey='{}'", businessKey);
    }

    /**
     * Chạy raw SQL để poll kết quả generic (dùng cho các assertion tuỳ chỉnh).
     */
    public <T> T pollUntilResult(String sql, Class<T> type, Object... args) {
        AtomicReference<T> ref = new AtomicReference<>();
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<T> results = jdbcTemplate.queryForList(sql, type, args);
                    if (!results.isEmpty()) {
                        ref.set(results.get(0));
                        return true;
                    }
                    return false;
                });
        return ref.get();
    }
}

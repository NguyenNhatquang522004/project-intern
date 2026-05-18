package com.example.demo.leavecore.delivery.JobWorker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.common.grpc.CreateApprovalHistoryRequest;
import com.example.demo.common.grpc.LeaveApprovalServiceGrpc.LeaveApprovalServiceBlockingStub;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.usecase.IUseCase.IHandleTaskEventUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@RequiredArgsConstructor
@Component
public class TaskCompleteWorker {
    private final IHandleTaskEventUseCase handleTaskEventUseCase;

    @GrpcClient("leaveinfrastructure")
    private LeaveApprovalServiceBlockingStub leaveApprovalServiceBlockingStub;

    // Định nghĩa hằng số các Header hệ thống của Camunda 8 Task Listener nhằm tránh
    // hardcode
    private static final String CAMUNDA_HEADER_EVENT_TYPE = "io.camunda.zeebe:taskListenerEventType";
    private static final String CAMUNDA_HEADER_ASSIGNEE = "io.camunda.zeebe:assignee";

    @JobWorker(type = "task-complete")
    public Map<String, Object> completeTask(final JobClient client, final ActivatedJob job) {
        log.info("--- START: completeTask Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(), job.getProcessInstanceKey());
        final Map<String, Object> outputVariables = new HashMap<String, Object>();
        try {
            // 1. Thu thập dữ liệu payload và headers
            Map<String, Object> variables = job.getVariablesAsMap();
            Map<String, String> customHeaders = job.getCustomHeaders();

            log.info("Extracted Process Variables: {}", variables);
            log.info("Extracted Custom Headers: {}", customHeaders);

            // 2. Map dữ liệu an toàn vào DTO
            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);

            // 3. Lấy trường eventType chuẩn chỉnh từ hệ thống Camunda 8
            String eventType = customHeaders.get(CAMUNDA_HEADER_EVENT_TYPE);
            log.info("Extracted eventType từ Custom Headers: {}", eventType);

            // Best Practice: Sử dụng so sánh Null-safe, không lo biến eventType bị null
            // if (!"completing".equalsIgnoreCase(eventType)) {
            //     log.warn("eventType '{}' không phải là 'completing'. Tiến hành đẩy lệnh BpmnError.", eventType);
            //     client.newThrowErrorCommand(job.getKey())
            //             .errorCode("400")
            //             .errorMessage("Task listener event is not completing")
            //             .send()
            //             .join();
            //     return outputVariables;
            // }

            // 4. Lấy thông tin người phê duyệt (Assignee) an toàn (Defensive Programming)
            // String approverEmail = customHeaders.get(CAMUNDA_HEADER_ASSIGNEE);
            // if (approverEmail == null && variables.get("assignee") != null) {
            //     approverEmail = variables.get("assignee").toString();
            // }
            // if (approverEmail == null || approverEmail.isBlank()) {
            //     approverEmail = "unknown_approver@company.com";
            //     log.warn("Không tìm thấy assignee trong cả Custom Headers lẫn Variables. Sử dụng fallback mặc định.");
            // }

            // 5. Xử lý nghiệp vụ lưu lịch sử duyệt qua gRPC dựa trên Trạng thái (Status)
            if (data.status() == LeaveRequestStatusEnum.REJECTED) {
                log.info("Trạng thái đơn: REJECTED. Thiết lập output variable Message.");
                outputVariables.put("Message", "Từ chối phê duyệt");
                outputVariables.put("Status", data.status().toString());
                CreateApprovalHistoryRequest historyRequest = CreateApprovalHistoryRequest.newBuilder()
                        .setLeaveRequestId(data.businessKey())
                        .setApproverEmail("")
                        .setAction("reject")
                        .build();

                // leaveApprovalServiceBlockingStub.recordApprovalHistory(historyRequest);

                // Hoàn thành Job của Task Listener kèm biến đầu ra
                client.newCompleteCommand(job.getKey()).send().join();
                
                log.info("--- END: completeTask Worker kết thúc thành công với trạng thái REJECTED ---");
                return outputVariables;
            }

            // Trường hợp APPROVED (Chấp thuận) hoặc các trạng thái khác
            CreateApprovalHistoryRequest historyRequest = CreateApprovalHistoryRequest.newBuilder()
                    .setLeaveRequestId(data.businessKey())
                    .setApproverEmail("")
                    .setAction("approval")
                    .build();

            // leaveApprovalServiceBlockingStub.recordApprovalHistory(historyRequest);
            outputVariables.put("Status", data.status().toString());
            log.info("Hoàn thành Task Listener bình thường.");
            client.newCompleteCommand(job.getKey()).send().join();
            log.info("--- END: completeTask Worker kết thúc thành công ---");
            return outputVariables;
        } catch (Exception e) {
            log.error("Exception nghiêm trọng xảy ra trong completeTask Worker: {}", e.getMessage(), e);

            // Đảm bảo thông báo lỗi chuẩn xác về Camunda Engine để giám sát viên dễ dàng xử
            // lý (Incident Management)
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(e.getMessage())
                    .send()
                    .join();
            log.info("Đã gửi lệnh Fail cho Job: {} | Số lần thử lại còn lại: {}", job.getKey(), job.getRetries() - 1);
            return outputVariables;
        }
    }
    // @JobWorker(type = "task-complete")
    // public void completeTask(final JobClient client, final ActivatedJob job) {
    // log.info("--- START: completeTask Worker ---");
    // log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(),
    // job.getProcessInstanceKey());
    // try {
    // Map<String, Object> variables = job.getVariablesAsMap();
    // log.info("Extracted Job Variables: {}", variables);

    // LeaveRequestCreateRequest data =
    // LeaveRequestRequest.mapToCreateRequest(variables);
    // log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);

    // String eventType = job.getCustomHeaders().get("zeebe:taskListenerEventType");
    // log.info("Extracted eventType: {}", eventType);

    // if (!eventType.equalsIgnoreCase("completing")) {
    // log.warn("eventType is not completing. Throwing Error command.");
    // client.newThrowErrorCommand(job.getKey())
    // .errorCode("400")
    // .errorMessage("Task is not completed")
    // .send()
    // .join();
    // return;
    // }
    // if (data.status() == LeaveRequestStatusEnum.REJECTED) {
    // log.info("Request status is REJECTED. Setting output variable Message: 'Từ
    // chối phê duyệt'");
    // final Map<String, Object> outputVariables = new HashMap<String, Object>();
    // outputVariables.put("Message", "Từ chối phê duyệt");
    // CreateApprovalHistoryRequest createApprovalHistoryRequest =
    // CreateApprovalHistoryRequest.newBuilder()
    // .setLeaveRequestId(data.businessKey())
    // .setApproverEmail(variables.get("assignee").toString())
    // .setAction("reject")
    // .build();
    // leaveApprovalServiceBlockingStub.recordApprovalHistory(createApprovalHistoryRequest);
    // client.newCompleteCommand(job.getKey()).variables(outputVariables).send().join();
    // log.info("--- END: completeTask Worker successfully completed with REJECTED
    // status ---");
    // return;
    // }

    // // gọi module cập nhật lịch sử
    // CreateApprovalHistoryRequest createApprovalHistoryRequest =
    // CreateApprovalHistoryRequest.newBuilder()
    // .setLeaveRequestId(data.businessKey())
    // .setApproverEmail(variables.get("assignee").toString())
    // .setAction("approval")
    // .build();
    // leaveApprovalServiceBlockingStub.recordApprovalHistory(createApprovalHistoryRequest);
    // log.info("Completing job normally");
    // client.newCompleteCommand(job.getKey()).send().join();
    // log.info("--- END: completeTask Worker successfully completed ---");
    // } catch (Exception e) {
    // log.error("Exception occurred in completeTask Worker: {}", e.getMessage(),
    // e);
    // client.newFailCommand(job.getKey()).retries(job.getRetries() -
    // 1).errorMessage(e.getMessage())
    // .send().join();
    // log.info("Failed job: {} with remaining retries: {}", job.getKey(),
    // job.getRetries() - 1);
    // }
    // }
}

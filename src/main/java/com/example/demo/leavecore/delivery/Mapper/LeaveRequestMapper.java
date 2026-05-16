package com.example.demo.leavecore.delivery.Mapper;

import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestUpdateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.domain.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "currentAssignee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    @Mapping(target = "currentAssigneeName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LeaveRequest toEntity(LeaveRequestCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "businessKey", ignore = true)
    @Mapping(target = "currentAssignee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(LeaveRequestUpdateRequest request, @MappingTarget LeaveRequest entity);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "currentAssignee.id", target = "currentAssigneeId")
    @Mapping(source = "leaveType.id", target = "leaveTypeId")
    LeaveRequestResponse toResponse(LeaveRequest entity);

}

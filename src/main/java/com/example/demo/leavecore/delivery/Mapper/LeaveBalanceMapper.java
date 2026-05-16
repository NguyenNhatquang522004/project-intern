package com.example.demo.leavecore.delivery.Mapper;

import com.example.demo.leavecore.delivery.Dto.LeaveBalance.LeaveBalanceRequest.LeaveBalanceCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveBalance.LeaveBalanceRequest.LeaveBalanceUpdateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveBalance.LeaveBalanceResponse;
import com.example.demo.leavecore.domain.entity.LeaveBalance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LeaveBalanceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    LeaveBalance toEntity(LeaveBalanceCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    void updateEntityFromRequest(LeaveBalanceUpdateRequest request, @MappingTarget LeaveBalance entity);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "leaveType.id", target = "leaveTypeId")
    LeaveBalanceResponse toResponse(LeaveBalance entity);

}

package com.example.demo.leavecore.delivery.Mapper;

import com.example.demo.leavecore.delivery.Dto.LeaveType.LeaveTypeRequest.LeaveTypeCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveType.LeaveTypeRequest.LeaveTypeUpdateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveType.LeaveTypeResponse;
import com.example.demo.leavecore.domain.entity.LeaveType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LeaveTypeMapper {

    @Mapping(target = "id", ignore = true)
    LeaveType toEntity(LeaveTypeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(LeaveTypeUpdateRequest request, @MappingTarget LeaveType entity);

    LeaveTypeResponse toResponse(LeaveType entity);

}

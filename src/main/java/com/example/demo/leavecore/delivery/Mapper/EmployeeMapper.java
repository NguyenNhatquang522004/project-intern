package com.example.demo.leavecore.delivery.Mapper;

import com.example.demo.leavecore.delivery.Dto.Employee.EmployeeRequest.EmployeeCreateRequest;
import com.example.demo.leavecore.delivery.Dto.Employee.EmployeeRequest.EmployeeUpdateRequest;
import com.example.demo.leavecore.delivery.Dto.Employee.EmployeeResponse;
import java.util.UUID;
import com.example.demo.leavecore.domain.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true) // Will be mapped in service
    @Mapping(target = "createdAt", ignore = true)
    Employee toEntity(EmployeeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(EmployeeUpdateRequest request, @MappingTarget Employee entity);

    @Mapping(source = "manager.id", target = "managerId")
    EmployeeResponse toResponse(Employee entity);

}

package com.example.demo.leavecore.delivery.Dto.Employee;

import java.util.UUID;
import jakarta.validation.constraints.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EmployeeRequest {
    public record EmployeeCreateRequest(
        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotNull(message = "Cấp bậc không được để trống")
        Integer positionLevel,

        @NotNull(message = "Phòng ban không được để trống")
        UUID departmentId,

        UUID managerId,


        String password ,
        
        String groupID,

        
        String status
    ) {}

    public record EmployeeUpdateRequest(
        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotNull(message = "Cấp bậc không được để trống")
        Integer positionLevel,

        @NotNull(message = "Phòng ban không được để trống")
        UUID departmentId,

        UUID managerId,

        String status
    ) {}
}

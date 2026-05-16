package com.example.demo.leavecore.delivery.Dto.Holiday;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HolidayRequest {
    public record HolidayCreateRequest(
        @NotNull(message = "Ngày nghỉ không được để trống")
        LocalDate holidayDate,

        String description
    ) {}

    public record HolidayUpdateRequest(
        @NotNull(message = "Ngày nghỉ không được để trống")
        LocalDate holidayDate,

        String description
    ) {}
}

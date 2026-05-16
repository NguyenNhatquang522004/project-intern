package com.example.demo.leavecore.delivery.Dto.Holiday;

import java.time.LocalDate;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {
    private Long id;
    private LocalDate holidayDate;
    private String description;
}

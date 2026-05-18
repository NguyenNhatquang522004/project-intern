package com.example.demo.leavecore.delivery.Dto.ManualTrigger;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record ValidateRequestOutput(
        Boolean isValid,
        BigDecimal totalWorkingDays,
        String businessKey,
        String message,
        String error
) {}

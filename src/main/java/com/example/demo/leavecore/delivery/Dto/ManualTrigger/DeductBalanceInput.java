package com.example.demo.leavecore.delivery.Dto.ManualTrigger;

import jakarta.validation.constraints.NotBlank;

public record DeductBalanceInput(
        @NotBlank(message = "Business Key không được để trống") String businessKey
) {}

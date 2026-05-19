package com.example.demo.common.Enum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountStatusEnum {
    PENDING,
    ACTIVE,
    INACTIVE
}

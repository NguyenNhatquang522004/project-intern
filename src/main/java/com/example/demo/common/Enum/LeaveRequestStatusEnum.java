package com.example.demo.common.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveRequestStatusEnum {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LeaveRequestStatusEnum fromValue(String value) {
        if (value == null || value.isBlank())
            return null;
        for (LeaveRequestStatusEnum e : values()) {
            if (e.value.equalsIgnoreCase(value))
                return e;
        }
        throw new IllegalArgumentException("Invalid value for LeaveRequestStatusEnum: '" + value + "'");
    }
}

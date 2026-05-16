package com.example.demo.common.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveTypeEnum {
    ANNUAL("ANNUAL"),
    SICK("SICK"),
    UNPAID("UNPAID");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LeaveTypeEnum fromValue(String value) {
        if (value == null || value.isBlank())
            return null;
        for (LeaveTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value))
                return e;
        }
        throw new IllegalArgumentException("Invalid value for LeaveTypeEnum: '" + value + "'");
    }
}

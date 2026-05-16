package com.example.demo.common.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveSessionEnum {
    MORNING("MORNING"), // Nghỉ buổi sáng (0.5)
    AFTERNOON("AFTERNOON"), // Nghỉ buổi chiều (0.5)
    ALL_DAY("ALL_DAY"); // Nghỉ cả ngày (1.0)

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LeaveSessionEnum fromValue(String value) {
        if (value == null || value.isBlank())
            return null;
        for (LeaveSessionEnum e : values()) {
            if (e.value.equalsIgnoreCase(value))
                return e;
        }
        throw new IllegalArgumentException("Invalid value for LeaveSessionEnum: '" + value + "'");
    }
}

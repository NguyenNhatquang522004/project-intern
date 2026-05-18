package com.example.demo.leavecore.delivery.Dto.manualtask;

public record UserTaskResponseDto(
        boolean success,
        String message,
        long timestamp,
        Object data
) {
    public static <T> UserTaskResponseDto success(String message, T data) {
        return new UserTaskResponseDto(true, message, System.currentTimeMillis(), data);
    }

    public static UserTaskResponseDto success(String message) {
        return new UserTaskResponseDto(true, message, System.currentTimeMillis(), null);
    }

    public static UserTaskResponseDto error(String message) {
        return new UserTaskResponseDto(false, message, System.currentTimeMillis(), null);
    }
}

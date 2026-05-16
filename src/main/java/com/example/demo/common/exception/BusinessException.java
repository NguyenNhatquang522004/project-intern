package com.example.demo.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends BaseAppException {
    private final String errorCode; 

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

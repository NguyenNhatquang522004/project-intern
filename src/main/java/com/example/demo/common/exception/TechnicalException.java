package com.example.demo.common.exception;

public class TechnicalException extends BaseAppException {

    public TechnicalException(String message, Throwable cause) {
        // Bây giờ lệnh gọi này sẽ hợp lệ 100%
        super(message, cause);
    }

    // Có thể thêm constructor này nếu bạn muốn ném lỗi mà không cần cause
    public TechnicalException(String message) {
        super(message);
    }
}

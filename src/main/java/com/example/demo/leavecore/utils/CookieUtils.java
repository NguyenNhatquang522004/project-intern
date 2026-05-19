package com.example.demo.leavecore.utils;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CookieUtils {

    public void setHttpOnlyCookie(HttpServletResponse response, String name, String value,
            long maxAgeInSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(maxAgeInSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .secure(false) // Đổi thành TRUE trên Production HTTPS
                .sameSite("Lax")
                .maxAge(0) // Đặt thời gian sống bằng 0 ép trình duyệt phải xóa ngay lập tức
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

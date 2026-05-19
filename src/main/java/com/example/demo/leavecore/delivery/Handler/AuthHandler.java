package com.example.demo.leavecore.delivery.Handler;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.auth.LoginResponse;
import com.example.demo.leavecore.utils.CookieUtils;

import jakarta.servlet.http.HttpServletResponse;

import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.LoginRequest;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.RegisterStep1Request;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.RegisterStep2Request;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.ResetPasswordRequest;
import com.example.demo.leavecore.usecase.adapterUseCase.AuthUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class AuthHandler {
    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> Login(@RequestBody LoginRequest request) {
        try {

            return ResponseEntity.ok(BaseResponse.<LoginResponse>builder()
                    .code("200")
                    .message("Đăng nhập thành công")
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<LoginResponse>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/register-step1")
    public ResponseEntity<BaseResponse<String>> RegisterStep1(@RequestBody RegisterStep1Request request) {
        try {
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("200")
                    .message("Đăng ký thành công")
                    .data("Đăng ký thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/register-step2")
    public ResponseEntity<BaseResponse<String>> RegisterStep2(@RequestBody RegisterStep2Request request) {
        try {
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("200")
                    .message("Đăng ký thành công")
                    .data("Đăng ký thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<String>> ResetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("200")
                    .message("Reset mật khẩu thành công")
                    .data("Reset mật khẩu thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(HttpServletResponse response) {
        try {
            CookieUtils.clearCookie(response, "access_token");
            CookieUtils.clearCookie(response, "refresh_token");
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("200")
                    .message("Đăng xuất và xóa session cookie thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }
}

package com.example.demo.leavecore.delivery.Handler;

import java.util.List;

import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.auth.AuthResponse;
import com.example.demo.leavecore.delivery.Dto.auth.LoginResponse;
import com.example.demo.leavecore.utils.CookieUtils;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.LoginRequest;
import com.example.demo.leavecore.delivery.Dto.keycloak.GroupResponse;
import com.example.demo.leavecore.delivery.Dto.auth.RegisterStep1Request;
import com.example.demo.leavecore.delivery.Dto.auth.RegisterStep2Request;
import com.example.demo.leavecore.delivery.Dto.auth.ResetPasswordRequest;
import com.example.demo.leavecore.usecase.adapterUseCase.AuthUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class AuthHandler {
    private final AuthUseCase authUseCase;

    @GetMapping("/group")
    public ResponseEntity<BaseResponse<List<GroupResponse>>> GetAllGroup() {
        try {
            log.info("Get all group");
            BaseResponse<List<GroupResponse>> response = authUseCase.GetAllGroup();
            log.info("Response: {}", response);
            if (!"200".equals(response.getCode())) {
                return ResponseEntity.ok(BaseResponse.<List<GroupResponse>>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(response.getData())
                        .build());
            }
            return ResponseEntity.ok(BaseResponse.<List<GroupResponse>>builder()
                    .code("200")
                    .message("Lấy danh sách group thành công")
                    .data(response.getData())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<List<GroupResponse>>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> Login(@Valid @RequestBody LoginRequest request,
            HttpServletResponse httpServletResponse) {
        try {
            BaseResponse<LoginResponse> response = authUseCase.Login(request);
            if (!"200".equals(response.getCode())) {
                return ResponseEntity.ok(BaseResponse.<LoginResponse>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(response.getData())
                        .build());
            }
            log.info("Responseaa: {}", response);
            CookieUtils.setHttpOnlyCookie(httpServletResponse, "access-token", response.getData().getAccessToken(),
                    response.getData().getExpiresIn());
            CookieUtils.setHttpOnlyCookie(httpServletResponse, "refresh-token", response.getData().getRefreshToken(),
                    response.getData().getRefreshExpiresIn());
            return ResponseEntity.ok(BaseResponse.<LoginResponse>builder()
                    .code("200")
                    .message("Đăng nhập thành công")
                    .data(response.getData())
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
    public ResponseEntity<BaseResponse<AuthResponse>> RegisterStep1(
            @Valid @RequestBody RegisterStep1Request request, HttpServletResponse httpServletResponse) {
        try {
            BaseResponse<AuthResponse> response = authUseCase.RegisterStep1(request);
            if (!response.getCode().equals("200")) {
                return ResponseEntity.ok(BaseResponse.<AuthResponse>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(response.getData())
                        .build());
            }

            return ResponseEntity.ok(BaseResponse.<AuthResponse>builder()
                    .code("200")
                    .message("Đăng ký thành công")
                    .data(response.getData())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<AuthResponse>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/register-step2")
    public ResponseEntity<BaseResponse<AuthResponse>> RegisterStep2(@Valid @RequestBody RegisterStep2Request request) {
        try {
            BaseResponse<AuthResponse> response = authUseCase.RegisterStep2(request);
            if (!response.getCode().equals("200")) {
                return ResponseEntity.ok(BaseResponse.<AuthResponse>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(response.getData())
                        .build());
            }
            return ResponseEntity.ok(BaseResponse.<AuthResponse>builder()
                    .code("200")
                    .message("Xác thực OTP thành công")
                    .data(response.getData())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.<AuthResponse>builder()
                    .code("500")
                    .message("Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép.")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<String>> ResetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            BaseResponse<String> response = authUseCase.ResetPassword(request);
            if (!response.getCode().equals("200")) {
                return ResponseEntity.ok(BaseResponse.<String>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(response.getData())
                        .build());
            }
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .code("200")
                    .message("Reset mật khẩu thành công")
                    .data(response.getData())
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
            CookieUtils.clearCookie(response, "access-token");
            CookieUtils.clearCookie(response, "refresh-token");
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

package com.example.demo.leavecore.delivery.Dto.auth;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthRequest {

    public record RegisterStep1Request(
            @NotBlank(message = "Họ tên không được để trống") String fullName,

            @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email,
            String password,

            String groupID,

            String status
        ) {}

    public record RegisterStep2Request(
            @NotBlank(message = "OTP không được để trống") String Otp,
            @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email) {
    }

    public record ResetPasswordRequest (
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email,
        @NotBlank(message =  "ClientID không được để trống") String clientId,
        @NotBlank(message = "redirectUri không được để trống") String redirectUri
    ) {

    }
    public record LoginRequest (
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email,
        @NotBlank(message = "Mật khẩu không được để trống") String password
    ) {

    }

}

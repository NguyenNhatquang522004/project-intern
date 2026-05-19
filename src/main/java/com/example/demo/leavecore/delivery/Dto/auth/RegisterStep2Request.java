package com.example.demo.leavecore.delivery.Dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStep2Request {
    @NotBlank(message = "OTP không được để trống")
    String Otp;
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;
}

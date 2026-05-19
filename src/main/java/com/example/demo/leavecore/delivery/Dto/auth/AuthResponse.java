package com.example.demo.leavecore.delivery.Dto.auth;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthResponse {
    private String Email;
    private String Otp;

}

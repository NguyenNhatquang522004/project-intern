package com.example.demo.leavecore.delivery.Dto.auth;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;

public class AuthRequest {


    

   
    public record LoginRequest(
            @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email,
            @NotBlank(message = "Mật khẩu không được để trống") String password) {

    }

}

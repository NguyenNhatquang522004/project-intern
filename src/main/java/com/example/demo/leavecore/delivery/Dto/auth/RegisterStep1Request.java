package com.example.demo.leavecore.delivery.Dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterStep1Request {
        @JsonProperty("fullName")
        @NotBlank(message = "Họ tên không được để trống")
        String fullName;

        @JsonProperty("email")
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email;

        @JsonProperty("password")
        String password;

        @JsonProperty("groupId")
        String groupID;
}

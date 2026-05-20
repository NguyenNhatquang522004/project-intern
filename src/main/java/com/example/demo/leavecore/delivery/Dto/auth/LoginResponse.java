package com.example.demo.leavecore.delivery.Dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    @JsonProperty("Email")
    private String Email;
    @JsonProperty("access_token")
    private String AccessToken;

    @JsonProperty("refresh_token")
    private String RefreshToken;

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("refresh_expires_in")
    private long refreshExpiresIn;

    @JsonProperty("token_type")
    private String TokenType;

    @JsonProperty("scope")
    private String Scope;

    @JsonProperty("FullName")
    private String FullName;

    @JsonProperty("id")
    private String id;
}

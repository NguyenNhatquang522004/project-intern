package com.example.demo.leavecore.usecase.IUseCase;

import java.util.List;

import org.keycloak.representations.idm.GroupRepresentation;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.LoginRequest;
import com.example.demo.leavecore.delivery.Dto.auth.RegisterStep1Request;
import com.example.demo.leavecore.delivery.Dto.auth.RegisterStep2Request;
import com.example.demo.leavecore.delivery.Dto.auth.ResetPasswordRequest;
import com.example.demo.leavecore.delivery.Dto.keycloak.GroupResponse;
import com.example.demo.leavecore.delivery.Dto.auth.AuthResponse;
import com.example.demo.leavecore.delivery.Dto.auth.LoginResponse;

public interface IAuthUseCase {
    BaseResponse<AuthResponse> RegisterStep1(RegisterStep1Request request);

    BaseResponse<AuthResponse> RegisterStep2(RegisterStep2Request request);

    BaseResponse<String> ResetPassword(ResetPasswordRequest request);

    BaseResponse<LoginResponse> Login(LoginRequest request);

   BaseResponse<List<GroupResponse>>  GetAllGroup();

}

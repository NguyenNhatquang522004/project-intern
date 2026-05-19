package com.example.demo.leavecore.domain.IRepository;

import java.util.List;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.Employee.EmployeeRequest.EmployeeCreateRequest;
import com.example.demo.leavecore.delivery.Dto.auth.LoginResponse;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.LoginRequest;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.RegisterStep1Request;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.ResetPasswordRequest;

public interface IRepositoryUser {

    BaseResponse<String> CreateGroup(String groupName);

    BaseResponse<String> DeleteGroup(String groupID);

    BaseResponse<String> CreateUser(RegisterStep1Request request);

    BaseResponse<String> AddUserGroup(String groupID, String userID);

    BaseResponse<String> RemoveUserGroup(String groupID, String userID);

    BaseResponse<Boolean> CheckUserGroup(String groupID, String userID);

    BaseResponse<List<GroupRepresentation>> GetAllGroup();

    BaseResponse<GroupRepresentation> GetGroupById(String groupId);

    BaseResponse<List<UserRepresentation>> GetAllUser();

    BaseResponse<UserRepresentation> GetUserByUserId(String userId);

    BaseResponse<UserRepresentation> GetUserByEmail(String email);

    BaseResponse<String> UpdateIsActiveUser(String email, boolean isActive);

    BaseResponse<String> sendResetPasswordEmail(ResetPasswordRequest request);

    LoginResponse Login(LoginRequest request);
}

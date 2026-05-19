package com.example.demo.leavecore.infrastructure.keycloak.Adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.Employee.EmployeeRequest.EmployeeCreateRequest;
import com.example.demo.leavecore.delivery.Dto.auth.LoginResponse;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.LoginRequest;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.RegisterStep1Request;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.ResetPasswordRequest;
import com.example.demo.leavecore.domain.IRepository.IRepositoryUser;

import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import io.camunda.common.auth.TokenResponse;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j

public class UserRepositoryImpl implements IRepositoryUser {
    private final WebClient webClient;
    private final Keycloak keycloak;
    @Value("${keycloak.admin.realm}")
    private final String realm;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    public UserRepositoryImpl(
            WebClient webClient,
            Keycloak keycloak,
            @Value("${keycloak.admin.realm}") String realm,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${keycloak.admin.client-id}") String clientId,
            @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.webClient = webClient;
        this.keycloak = keycloak;
        this.realm = realm;
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private RealmResource getRealmResource() {
        return keycloak.realm(realm);
    }

    public BaseResponse<String> CreateGroup(String groupName) {
        try {
            GroupRepresentation group = new GroupRepresentation();
            group.setName(groupName);
            Response response = getRealmResource().groups().add(group);
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                return BaseResponse.<String>builder().code("400").data("")
                        .message(response.getStatus() + "" + " không tạo thành công ")
                        .build();
            }
            return BaseResponse.<String>builder().code("200").data("").message("thành công ").build();
        } catch (Exception e) {
            return BaseResponse.<String>builder().code("400").data("").message(e.getMessage()).build();
        }
    }

    public BaseResponse<String> DeleteGroup(String groupID) {
        try {
            GroupRepresentation group = getRealmResource().groups().group(groupID).toRepresentation();
            if (group == null) {
                return BaseResponse.<String>builder().code("404").data("").message("không tìm thấy ").build();
            }
            getRealmResource().groups().group(groupID).remove();
            return BaseResponse.<String>builder().code("200").data("").message("thành công ").build();
        } catch (Exception e) {
            return BaseResponse.<String>builder().code("400").data("").message(e.getMessage()).build();
        }
    }

    public BaseResponse<String> CreateUser(RegisterStep1Request request) {
        try {

            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.email());
            user.setEmail(request.email());
            user.setFirstName(request.fullName());
            user.setLastName(request.fullName());
            user.setEnabled(false);
            user.setEmailVerified(false);
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("Email", java.util.Collections.singletonList(request.email()));
            attributes.put("FullName", java.util.Collections.singletonList(request.fullName()));
            user.setAttributes(attributes);
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setValue(request.password());
            credential.setTemporary(false);
            user.setCredentials(java.util.Collections.singletonList(credential));
            BaseResponse<GroupRepresentation> GetGroupById = GetGroupById(request.groupID());
            if (GetGroupById.getCode() != "200") {
                return BaseResponse.<String>builder().code("400").data("")
                        .message(GetGroupById.getMessage() + "" + " không tìm thấy group ")
                        .build();
            }
            BaseResponse<String> AddUserGroup = AddUserGroup(request.groupID(), user.getId());
            if (AddUserGroup.getCode() != "200") {
                return BaseResponse.<String>builder().code("400").data("")
                        .message(AddUserGroup.getMessage() + "" + " không tạo thành công ")
                        .build();
            }
            Response response = getRealmResource().users().create(user);
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                return BaseResponse.<String>builder().code("400").data("")
                        .message(response.getStatus() + "" + " không tạo thành công ")
                        .build();
            }
            return BaseResponse.<String>builder().code("200").data("").message("thành công ").build();
        } catch (Exception e) {
            return BaseResponse.<String>builder().code("400").data("").message(e.getMessage()).build();
        }
    }

    public BaseResponse<String> AddUserGroup(String groupID, String userID) {
        try {
            getRealmResource().users().get(userID).joinGroup(groupID);
            return BaseResponse.<String>builder().code("200").data("").message("thành công ").build();
        } catch (Exception e) {
            return BaseResponse.<String>builder().code("400").data("").message(e.getMessage()).build();
        }
    }

    public BaseResponse<String> RemoveUserGroup(String groupID, String userID) {
        try {
            getRealmResource().users().get(userID).leaveGroup(groupID);
            return BaseResponse.<String>builder().code("200").data("").message("thành công ").build();
        } catch (Exception e) {
            return BaseResponse.<String>builder().code("400").data("").message(e.getMessage()).build();
        }
    }

    public BaseResponse<Boolean> CheckUserGroup(String groupID, String userID) {
        try {
            List<GroupRepresentation> groups = getRealmResource().users().get(userID).groups();
            for (GroupRepresentation group : groups) {
                if (group.getId().equals(groupID)) {
                    return BaseResponse.<Boolean>builder().code("200").data(true).message("thành công ").build();
                }
            }
            return BaseResponse.<Boolean>builder().code("200").data(false).message("thành công ").build();
        } catch (Exception e) {
            return BaseResponse.<Boolean>builder().code("400").data(false).message(e.getMessage()).build();
        }
    }

    public BaseResponse<List<GroupRepresentation>> GetAllGroup() {
        try {
            List<GroupRepresentation> groups = getRealmResource().groups().groups();
            return BaseResponse.<List<GroupRepresentation>>builder().code("200").data(groups).message("thành công ")
                    .build();
        } catch (Exception e) {
            return BaseResponse.<List<GroupRepresentation>>builder().code("400").data(null)
                    .message(e.getMessage()).build();
        }
    }

    public BaseResponse<GroupRepresentation> GetGroupById(String groupId) {
        try {
            GroupRepresentation group = getRealmResource().groups().group(groupId).toRepresentation();
            return BaseResponse.<GroupRepresentation>builder().code("200").data(group).message("thành công ")
                    .build();
        } catch (Exception e) {
            return BaseResponse.<GroupRepresentation>builder().code("400").data(null)
                    .message(e.getMessage()).build();
        }
    }

    public BaseResponse<List<UserRepresentation>> GetAllUser() {
        try {
            List<UserRepresentation> users = getRealmResource().users().list();
            return BaseResponse.<List<UserRepresentation>>builder().code("200").data(users).message("thành công ")
                    .build();
        } catch (Exception e) {
            return BaseResponse.<List<UserRepresentation>>builder().code("400").data(null)
                    .message(e.getMessage()).build();
        }
    }

    public BaseResponse<UserRepresentation> GetUserByUserId(String userId) {
        try {
            UserRepresentation user = getRealmResource().users().get(userId).toRepresentation();
            return BaseResponse.<UserRepresentation>builder().code("200").data(user).message("thành công ")
                    .build();
        } catch (Exception e) {
            return BaseResponse.<UserRepresentation>builder().code("400").data(null)
                    .message(e.getMessage()).build();
        }
    }

    public BaseResponse<UserRepresentation> GetUserByEmail(String email) {
        try {
            List<UserRepresentation> users = getRealmResource().users().search(email);
            if (users.isEmpty()) {
                return BaseResponse.<UserRepresentation>builder().code("404").data(null).message("không tìm thấy ")
                        .build();
            }
            return BaseResponse.<UserRepresentation>builder().code("200").data(users.get(0)).message("thành công ")
                    .build();
        } catch (Exception e) {
            return BaseResponse.<UserRepresentation>builder().code("400").data(null)
                    .message(e.getMessage()).build();
        }
    }

    public BaseResponse<String> UpdateIsActiveUser(String email, boolean isActive) {
        try {
            List<UserRepresentation> users = getRealmResource().users().search(email);
            if (users.isEmpty()) {
                return BaseResponse.<String>builder().code("404").data("").message("không tìm thấy ")
                        .build();
            }
            UserRepresentation user = users.get(0);
            user.setEnabled(isActive);
            getRealmResource().users().get(user.getId()).update(user);
            return BaseResponse.<String>builder().code("200").data("").message("thành công ").build();
        } catch (Exception e) {
            return BaseResponse.<String>builder().code("400").data("").message(e.getMessage()).build();
        }
    }

    @Override
    public BaseResponse<String> sendResetPasswordEmail(ResetPasswordRequest request) {
        try {
            UserResource userResource = getRealmResource().users().get(request.email());
            if (userResource == null) {
                return BaseResponse.<String>builder().code("404").data("").message("không tìm thấy ").build();
            }
            List<String> actions = List.of("UPDATE_PASSWORD");
            userResource.executeActionsEmail(request.clientId(), request.redirectUri(), actions);
            return BaseResponse.<String>builder().code("200").data("").message("thành công").build();

        } catch (Exception e) {
            return BaseResponse.<String>builder().code("400").data("").message(e.getMessage()).build();
        }
    }

    private LoginResponse buildCustomResponse(Map<String, Object> rawTokenData) {
        return LoginResponse.builder()
                .AccessToken((String) rawTokenData.get("access_token"))
                .RefreshToken((String) rawTokenData.get("refresh_token"))
                .expiresIn(((Number) rawTokenData.get("expires_in")).longValue())
                .refreshExpiresIn(((Number) rawTokenData.get("refresh_expires_in")).longValue())
                .TokenType((String) rawTokenData.get("token_type"))
                .Scope((String) rawTokenData.get("scope"))
                .build();
    }

    @Override
    public LoginResponse Login(LoginRequest request) {
        try {
            List<UserRepresentation> users = getRealmResource().users().search(request.email());
            if (users.isEmpty()) {
                return null;
            }
            String tokenEndpoint = issuerUri + "/protocol/openid-connect/token";
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("username", request.email());
            formData.add("password", request.password());
            formData.add("scope", "openid profile email");
            return webClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromValue(formData))
                    .retrieve()
                    .bodyToMono(LoginResponse.class) // Jackson tự động mapping dựa trên @JsonProperty
                    .block();
        } catch (Exception e) {
            log.error("Error login: {}", e.getMessage());
            return null;
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        String tokenEndpoint = issuerUri + "/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        return executeTokenRequest(tokenEndpoint, formData);
    }

    private TokenResponse executeTokenRequest(String endpoint, MultiValueMap<String, String> formData) {
        try {
            return webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED) // Bắt buộc đúng Content-Type
                    .body(BodyInserters.fromValue(formData))
                    .retrieve()
                    .bodyToMono(TokenResponse.class) // Tự động ánh xạ chuỗi JSON nhận được về Record DTO của bạn
                    .block(); // Đưa về xử lý đồng bộ (Synchronous) phù hợp với Spring MVC truyền thống
        } catch (WebClientResponseException.Unauthorized e) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!");
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Lỗi hệ thống xác thực từ Keycloak: " + e.getResponseBodyAsString());
        }
    }
}

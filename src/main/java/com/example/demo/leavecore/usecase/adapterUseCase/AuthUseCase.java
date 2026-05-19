package com.example.demo.leavecore.usecase.adapterUseCase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.common.Dto.EmailRequest;
import com.example.demo.common.Enum.AccountStatusEnum;
import com.example.demo.common.share.email.IEmail;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.LoginRequest;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.RegisterStep1Request;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.RegisterStep2Request;
import com.example.demo.leavecore.delivery.Dto.auth.AuthRequest.ResetPasswordRequest;
import com.example.demo.leavecore.domain.IRepository.IRepositoryEmployee;
import com.example.demo.leavecore.domain.IRepository.IRepositoryUser;
import com.example.demo.leavecore.domain.entity.Employee;
import com.example.demo.leavecore.delivery.Dto.auth.AuthResponse;
import com.example.demo.leavecore.delivery.Dto.auth.LoginResponse;
import com.example.demo.leavecore.usecase.IUseCase.IAuthUseCase;
import com.example.demo.leavecore.utils.OtpUtils;
import com.example.demo.leavecore.utils.PasswordUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthUseCase implements IAuthUseCase {
    private final IRepositoryUser repositoryUser;
    private final IEmail emailService;
    private final IRepositoryEmployee repositoryEmployee;
    private final PasswordUtils passwordUtils;

    @Override
    public BaseResponse<AuthResponse> RegisterStep1(RegisterStep1Request request) {
        try {
            BaseResponse<UserRepresentation> response = repositoryUser.GetUserByEmail(request.email());
            if (response.getCode() != null && response.getCode() != "200") {
                return BaseResponse.<AuthResponse>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(null)
                        .build();
            }
            BaseResponse<String> response2 = repositoryUser.CreateUser(request);
            if (response2.getCode() != null && response2.getCode() != "200") {
                return BaseResponse.<AuthResponse>builder()
                        .code(response2.getCode())
                        .message(response2.getMessage())
                        .data(null)
                        .build();
            }
            String otp = OtpUtils.generateNumericOtp(6);
            String encodePassword = passwordUtils.encodePassword(request.password());
            UserRepresentation user = response.getData();
            Employee employee = Employee.builder()
                    .id(UUID.fromString(user.getId()))
                    .email(user.getEmail())
                    .fullName(request.fullName())
                    .status(AccountStatusEnum.INACTIVE)
                    .password(encodePassword)
                    .codeOTP(otp)
                    .countResendEmail(0)
                    .codeOtpExpiryDate(LocalDateTime.now().plusMinutes(30))
                    .build();
            repositoryEmployee.save(employee);
            Map<String, Object> variables = new HashMap<>();
            variables.put("username", request.fullName());
            variables.put("otpCode", otp);
            variables.put("expiryMinutes", "30");
            BaseResponse<Void> response3 = sendOtpEmail(EmailRequest.builder()
                    .to(request.email())
                    .subject("Mã xác thực OTP")
                    .templateName("emails/otp-template")
                    .variables(variables)
                    .build());

            return BaseResponse.<AuthResponse>builder()
                    .code("200")
                    .message("Thành công")
                    .data(null)
                    .build();
        } catch (Exception e) {
            return BaseResponse.<AuthResponse>builder()
                    .code("500")
                    .message(e.getMessage())
                    .data(null)
                    .build();
        }
    }

    private BaseResponse<Void> sendOtpEmail(EmailRequest request) {
        try {
            emailService.sendEmail(request);
            return BaseResponse.<Void>builder()
                    .code("200")
                    .message("Thành công")
                    .data(null)
                    .build();
        } catch (Exception e) {
            return BaseResponse.<Void>builder()
                    .code("500")
                    .message(e.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Override
    public BaseResponse<AuthResponse> RegisterStep2(RegisterStep2Request request) {
        try {
            BaseResponse<UserRepresentation> response = repositoryUser.GetUserByEmail(request.email());
            if (response.getCode() != null && response.getCode() != "200") {
                return BaseResponse.<AuthResponse>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(null)
                        .build();
            }
            Optional<Employee> employee = repositoryEmployee.findByEmail(request.email());
            if (employee.isEmpty()) {
                return BaseResponse.<AuthResponse>builder()
                        .code("404")
                        .message("Không tìm thấy nhân viên")
                        .data(null)
                        .build();
            }
            if (request.Otp() != employee.get().getCodeOTP()) {
                employee.get().setCountFailOtp(employee.get().getCountFailOtp() + 1);
                if (employee.get().getCountFailOtp() > 3) {
                    return BaseResponse.<AuthResponse>builder()
                            .code("400")
                            .message("Số lần nhập mã OTP vượt quá giới hạn")
                            .data(null)
                            .build();
                }
                return BaseResponse.<AuthResponse>builder()
                        .code("400")
                        .message("Sai mã OTP")
                        .data(null)
                        .build();
            }
            if (LocalDateTime.now().isAfter(employee.get().getCodeOtpExpiryDate())) {
                return BaseResponse.<AuthResponse>builder()
                        .code("400")
                        .message("Mã OTP đã hết hạn")
                        .data(null)
                        .build();
            }
            employee.get().setCountFailOtp(0);
            employee.get().setStatus(AccountStatusEnum.ACTIVE);
            employee.get().setCodeOTP(null);
            employee.get().setCodeOtpExpiryDate(null);
            BaseResponse<String> response3 = repositoryUser.UpdateIsActiveUser(employee.get().getEmail(), true);
            if (response3.getCode() != null && response3.getCode() != "200") {
                return BaseResponse.<AuthResponse>builder()
                        .code(response3.getCode())
                        .message(response3.getMessage())
                        .data(null)
                        .build();
            }
            repositoryEmployee.save(employee.get());
            return BaseResponse.<AuthResponse>builder()
                    .code("200")
                    .message("Thành công")
                    .data(null)
                    .build();
        } catch (Exception e) {
            return BaseResponse.<AuthResponse>builder()
                    .code("500")
                    .message(e.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Override
    public BaseResponse<String> ResetPassword(ResetPasswordRequest request) {
        try {
            BaseResponse<UserRepresentation> response = repositoryUser.GetUserByEmail(request.email());
            if (response.getCode() != null && response.getCode() != "200") {
                return BaseResponse.<String>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(null)
                        .build();
            }
            Optional<Employee> employee = repositoryEmployee.findByEmail(request.email());
            if (employee.isEmpty()) {
                return BaseResponse.<String>builder()
                        .code("404")
                        .message("Không tìm thấy nhân viên")
                        .data(null)
                        .build();
            }
            if (employee.get().getCountResendEmail() > 3) {
                return BaseResponse.<String>builder()
                        .code("400")
                        .message("Số lần gửi lại mã OTP đã vượt quá giới hạn")
                        .data(null)
                        .build();
            }
            employee.get().setCountResendEmail(employee.get().getCountResendEmail() + 1);
            repositoryEmployee.save(employee.get());
            BaseResponse<String> response2 = repositoryUser.sendResetPasswordEmail(request);
            if (response2.getCode() != null && response2.getCode() != "200") {
                return BaseResponse.<String>builder()
                        .code(response2.getCode())
                        .message(response2.getMessage())
                        .data(null)
                        .build();
            }
            return BaseResponse.<String>builder()
                    .code("200")
                    .message("Thành công")
                    .data(null)
                    .build();
        } catch (Exception e) {
            return BaseResponse.<String>builder()
                    .code("500")
                    .message(e.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Override
    public BaseResponse<LoginResponse> Login(LoginRequest request) {
        try {
            BaseResponse<UserRepresentation> response = repositoryUser.GetUserByEmail(request.email());
            if (response.getCode() != null && response.getCode() != "200") {
                return BaseResponse.<LoginResponse>builder()
                        .code(response.getCode())
                        .message(response.getMessage())
                        .data(null)
                        .build();
            }
            Optional<Employee> employee = repositoryEmployee.findByEmail(request.email());
            if (employee.isEmpty()) {
                return BaseResponse.<LoginResponse>builder()
                        .code("404")
                        .message("Không tìm thấy nhân viên")
                        .data(null)
                        .build();
            }
            boolean checkpassword = passwordUtils.verifyPassword(request.password(), employee.get().getPassword());
            if (!checkpassword) {
                return BaseResponse.<LoginResponse>builder()
                        .code("400")
                        .message("Sai mật khẩu")
                        .data(null)
                        .build();
            }
            boolean checkIsActive = employee.get().getStatus().equals("ACTIVE");
            if (!checkIsActive) {
                return BaseResponse.<LoginResponse>builder()
                        .code("400")
                        .message("Nhân viên không hoạt động")
                        .data(null)
                        .build();
            }
            LoginResponse loginResponse = repositoryUser.Login(request);
            if (loginResponse.getAccessToken() == null) {
                return BaseResponse.<LoginResponse>builder()
                        .code("400")
                        .message("Sai mật khẩu hoặc email")
                        .data(null)
                        .build();
            }
            loginResponse.setEmail(request.email());
            loginResponse.setFullName(employee.get().getFullName());
            return BaseResponse.<LoginResponse>builder()
                    .code("200")
                    .message("Thành công")

                    .data(null)
                    .build();
        } catch (Exception e) {
            return BaseResponse.<LoginResponse>builder()
                    .code("500")
                    .message(e.getMessage())
                    .data(null)
                    .build();
        }
    }
}

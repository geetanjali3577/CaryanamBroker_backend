package com.caryanam.caryanam_broker.service;

import com.caryanam.caryanam_broker.dto.*;


public interface AuthService {

    RegisterResponseDTO registerUser(RegisterRequestDTO dto);

    RegisterResponseDTO registerPropertyOwner(RegisterRequestDTO dto);

    public String login(LoginRequestDTO dto);

    void logout(String token);

    Object updateUser(Long id, RegisterRequestDTO dto);

    Object updateAdmin(Long id, RegisterRequestDTO dto);

    Object updateOwner(Long id, RegisterRequestDTO dto);

    boolean deactivateOwner(Long id);

    boolean deactivateAdmin(Long id);

    boolean deactivateUser(Long id);
    void sendForgotPasswordOtp(ForgotPasswordRequestDTO dto);

    boolean verifyOtp(VerifyOtpDTO dto);

    boolean resetPassword(ResetPasswordDTO dto);
    void sendEmailVerificationOtp(SendEmailOtpDTO dto);

    boolean verifyEmailOtp(VerifyEmailOtpDTO dto);
}

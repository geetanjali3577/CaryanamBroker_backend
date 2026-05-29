package com.caryanam.caryanam_broker.controller;


import com.caryanam.caryanam_broker.dto.*;
import com.caryanam.caryanam_broker.messageconfig.MessageConfig;
import com.caryanam.caryanam_broker.repository.AdminRepository;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import com.caryanam.caryanam_broker.service.AuthService;

import lombok.RequiredArgsConstructor;
import com.caryanam.caryanam_broker.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;



    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;

    @PostMapping("/register/user")
    public ResponseEntity<ResponseDto<RegisterResponseDTO>> registerUser(
            @RequestBody RegisterRequestDTO dto) {

        if (dto == null) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Request body is missing", null));
        }
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Full name is required", null));
        }
        if (!dto.getFullName().matches("^[A-Za-z ]+$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Full name must contain only letters and spaces", null));
        }
        if (dto.getMobileNumber() == null || !dto.getMobileNumber().matches("\\d{10}")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number must be 10 digits", null));
        }
        if (dto.getMobileNumber() == null ||
                !dto.getMobileNumber().matches("^[6-9]\\d{9}$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number must be Starts with 6, 7, 8, or 9", null));
        }
        if (dto.getMobileNumber() == null || dto.getMobileNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number is required", null));
        }
        if (userRepository.existsByMobileNumber(dto.getMobileNumber())) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number already exists", null));
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Email is required", null));
        }
        if (!dto.getEmail().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Only Gmail format allowed (example: user@gmail.com)", null));
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Password is required", null));
        }
        if (dto.getRole() == null) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Role is required", null));
        }
        dto.setEmail(dto.getEmail().toLowerCase().trim());
        RegisterResponseDTO response = authService.registerUser(dto);
        return ResponseEntity.status(201).body(new ResponseDto<>(201, "User Registered Successfully", response));
    }

    @PostMapping("/register/POwner")
    public ResponseEntity<ResponseDto<RegisterResponseDTO>> registerOwner(
            @RequestBody RegisterRequestDTO dto) {

        if (dto == null) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Request body is missing", null));
        }
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Full name is required", null));
        }
        if (!dto.getFullName().matches("^[a-zA-Z\\s.-]+$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Full name contains invalid characters", null));
        }
        if (dto.getMobileNumber() == null || !dto.getMobileNumber().matches("\\d{10}")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number must be 10 digits", null));
        }
        if (dto.getMobileNumber() == null ||
                !dto.getMobileNumber().matches("^[6-9]\\d{9}$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number must be Starts with 6, 7, 8, or 9", null));
        }
        if (dto.getMobileNumber() == null || dto.getMobileNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number is required", null));
        }
        if (propertyOwnerRepository.existsByMobileNumber(dto.getMobileNumber())) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Mobile number already exists", null));
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Email is required", null));
        }
        if (!dto.getEmail().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Only Gmail format allowed", null));
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Password is required", null));
        }
        if (dto.getRole() == null) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Role is required", null));
        }
        if (dto.getRole() != Role.PROPERTY_OWNER) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Only PROPERTY_OWNER role allowed", null));
        }

        dto.setEmail(dto.getEmail().toLowerCase().trim());
        RegisterResponseDTO response = authService.registerPropertyOwner(dto);
        return ResponseEntity.status(201).body(new ResponseDto<>(201, "Property Owner Registered Successfully", response));
    }



    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody LoginRequestDTO request) {

        if (request == null) {
            return ResponseEntity.badRequest().body(new LoginResponseDTO(400, "Request body is missing", null));
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new LoginResponseDTO(400, "Email is required", null));
        }
        if (!request.getEmail().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            return ResponseEntity.badRequest().body(new LoginResponseDTO(400, "Invalid email (only gmail format allowed)", null));
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new LoginResponseDTO(400, "Password is required", null));
        }
        if (request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(new LoginResponseDTO(400, "Password must be at least 6 characters", null));
        }
        request.setEmail(request.getEmail().toLowerCase().trim());
        String token = authService.login(request);
        return ResponseEntity.ok(new LoginResponseDTO(200, "Login Successful", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Authorization token is missing", null));
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return ResponseEntity.ok(
                new ResponseDto<>(200, "Logged out successfully", null));
    }


    @PutMapping("/update/user/{id}")
    public ResponseEntity<ResponseDto<?>> updateUser(@PathVariable Long id, @RequestBody RegisterRequestDTO dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.INVALID_ID, null));
        }
        if (dto.getFullName() != null && !dto.getFullName().matches("^[A-Za-z ]+$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.NAME_INVALID, null));
        }
        if (dto.getMobileNumber() != null && !dto.getMobileNumber().matches("\\d{10}")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.MOBILE_INVALID, null));
        }
        if (dto.getEmail() != null && !dto.getEmail().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.EMAIL_INVALID, null));
        }
        if (dto.getPassword() != null && dto.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.PASSWORD_INVALID, null));
        }
        Object res = authService.updateUser(id, dto);
        if (res == null) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.USER_NOT_FOUND, null));
        }
        return ResponseEntity.ok(new ResponseDto<>(200, MessageConfig.USER_UPDATED, res));
    }






    @PutMapping("/update/admin/{id}")
    public ResponseEntity<ResponseDto<?>> updateAdmin(@PathVariable Long id, @RequestBody RegisterRequestDTO dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.INVALID_ID, null));
        }
        if (dto.getFullName() != null &&
                !dto.getFullName().matches("^[A-Za-z ]+$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.NAME_INVALID, null));
        }
        if (dto.getMobileNumber() != null && !dto.getMobileNumber().matches("\\d{10}")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.MOBILE_INVALID, null));
        }
        if (dto.getEmail() != null && !dto.getEmail().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.EMAIL_INVALID, null));
        }
        if (dto.getPassword() != null && dto.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.PASSWORD_INVALID, null));
        }
        Object res = authService.updateAdmin(id, dto);
        if (res == null) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.ADMIN_NOT_FOUND, null));
        }
        return ResponseEntity.ok(new ResponseDto<>(200, MessageConfig.ADMIN_UPDATED, res));
    }

    @PutMapping("/update/owner/{id}")
    public ResponseEntity<ResponseDto<?>> updateOwner(@PathVariable Long id, @RequestBody RegisterRequestDTO dto) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.INVALID_ID, null));
        }
        if (dto.getFullName() != null && !dto.getFullName().matches("^[A-Za-z ]+$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.NAME_INVALID, null));
        }
        if (dto.getMobileNumber() != null && !dto.getMobileNumber().matches("\\d{10}")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.MOBILE_INVALID, null));
        }
        if (dto.getEmail() != null && !dto.getEmail().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.EMAIL_INVALID, null));
        }
        if (dto.getPassword() != null && dto.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.PASSWORD_INVALID, null));
        }
        Object res = authService.updateOwner(id, dto);
        if (res == null) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, MessageConfig.OWNER_NOT_FOUND, null));
        }
        return ResponseEntity.ok(new ResponseDto<>(200, MessageConfig.OWNER_UPDATED, res));
    }





    @PutMapping("/deactivate/user/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid ID");
        }
        boolean result = authService.deactivateUser(id);
        if (!result) {
            return ResponseEntity.badRequest().body("User not found");
        }
        return ResponseEntity.ok("User deactivated successfully");
    }



    @PutMapping("/deactivate/owner/{id}")
    public ResponseEntity<?> deactivateOwner(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid ID");
        }
        boolean result = authService.deactivateOwner(id);
        if (!result) {
            return ResponseEntity.badRequest().body("User not found");
        }
        return ResponseEntity.ok("User deactivated successfully");
    }




    @PutMapping("/deactivate/admin/{id}")
    public ResponseEntity<?> deactivateAdmin(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid ID");
        }
        boolean result = authService.deactivateAdmin(id);
        if (!result) {
            return ResponseEntity.badRequest().body("User not found");
        }
        return ResponseEntity.ok("User deactivated successfully");
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody VerifyOtpDTO dto) {

        boolean result = authService.verifyOtp(dto);

        if (!result) {

            return ResponseEntity.badRequest().body(
                    new ResponseDto<>(400,
                            "Invalid OTP",
                            null));
        }

        return ResponseEntity.ok(
                new ResponseDto<>(200,
                        "OTP verified successfully",
                        null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordDTO dto) {

        boolean result =
                authService.resetPassword(dto);

        if (!result) {

            return ResponseEntity.badRequest().body(
                    new ResponseDto<>(400,
                            "Password reset failed",
                            null));
        }

        return ResponseEntity.ok(
                new ResponseDto<>(200,
                        "Password updated successfully",
                        null));
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO dto) {

        authService.sendForgotPasswordOtp(dto);

        return ResponseEntity.ok(
                new ResponseDto<>(200,
                        "OTP sent successfully",
                        null));
    }
    @PostMapping("/send-email-otp")
    public ResponseEntity<?> sendEmailOtp(
            @RequestBody SendEmailOtpDTO dto) {

        authService.sendEmailVerificationOtp(dto);

        return ResponseEntity.ok(
                new ResponseDto<>(200,
                        "OTP sent successfully",
                        null));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<?> verifyEmailOtp(
            @RequestBody VerifyEmailOtpDTO dto) {

        boolean result =
                authService.verifyEmailOtp(dto);

        if (!result) {

            return ResponseEntity.badRequest().body(
                    new ResponseDto<>(400,
                            "Invalid OTP",
                            null));
        }

        return ResponseEntity.ok(
                new ResponseDto<>(200,
                        "Email verified successfully",
                        null));
    }
}

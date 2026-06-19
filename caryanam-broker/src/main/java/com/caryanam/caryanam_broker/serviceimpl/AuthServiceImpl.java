package com.caryanam.caryanam_broker.serviceimpl;


import com.caryanam.caryanam_broker.configuration.JwtUtil;
import com.caryanam.caryanam_broker.dto.*;
import com.caryanam.caryanam_broker.entity.*;
import com.caryanam.caryanam_broker.enums.Role;
import com.caryanam.caryanam_broker.repository.*;
import com.caryanam.caryanam_broker.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PropertyOwnerRepository propertyOwerRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;


    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;

    @Autowired
    private ForgotPasswordOtpRepository forgotPasswordOtpRepository;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailVerificationOtpRepository emailVerificationOtpRepository;

    private static final Set<String> tokenBlacklist = new HashSet<>();

    //  USER REGISTRATION
    @Override
    public RegisterResponseDTO registerUser(RegisterRequestDTO dto) {

        if (isEmailAlreadyUsed(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        EmailVerificationOtp emailOtp =
                emailVerificationOtpRepository.findByEmail(dto.getEmail());

        if (emailOtp == null || !emailOtp.isVerified()) {
            throw new RuntimeException("Please verify email first");
        }
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setMobileNumber(String.valueOf(dto.getMobileNumber()));
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        user.setIsActive("true");
        User saved = userRepository.save(user);

        return RegisterResponseDTO.builder()
                .id(saved.getUserId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole().name())

                .build();
    }

    @Override
    public RegisterResponseDTO registerPropertyOwner(RegisterRequestDTO dto) {
        if (isEmailAlreadyUsed(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        EmailVerificationOtp emailOtp =
                emailVerificationOtpRepository.findByEmail(dto.getEmail());

        if (emailOtp == null || !emailOtp.isVerified()) {
            throw new RuntimeException("Please verify email first");
        }
        PropertyOwner owner = new PropertyOwner();
        owner.setFullName(dto.getFullName());
        owner.setMobileNumber(String.valueOf(dto.getMobileNumber()));
        owner.setEmail(dto.getEmail());
        owner.setEmail(dto.getEmail());
        owner.setPassword(passwordEncoder.encode(dto.getPassword()));
        owner.setRole(Role.PROPERTY_OWNER);
        owner.setIsActive("true");
        PropertyOwner saved = propertyOwerRepository.save(owner);
        return RegisterResponseDTO.builder()
                .id(saved.getOwnerId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .build();
    }



    @Override
    public String login(LoginRequestDTO request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        String email =
                request.getEmail()
                        .toLowerCase()
                        .trim();

        // USER CHECK
        boolean isUser =
                userRepository
                        .findByEmail(email)
                        .isPresent();

        // OWNER CHECK
        boolean isOwner =
                propertyOwnerRepository
                        .findByEmail(email)
                        .isPresent();

        // EMAIL VERIFICATION ONLY FOR USER & OWNER
        if (isUser || isOwner) {

            EmailVerificationOtp emailOtp =
                    emailVerificationOtpRepository
                            .findByEmail(email);

            if (emailOtp == null ||
                    !Boolean.TRUE.equals(
                            emailOtp.isVerified())) {

                throw new RuntimeException(
                        "Email not verified"
                );
            }
        }

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String role =
                userDetails.getAuthorities()
                        .stream()
                        .findFirst()
                        .map(granted ->
                                granted.getAuthority())
                        .orElse("USER");

        Long id = null;

        String fullName = null;

        if ("ROLE_USER".equals(role)) {

            User user =
                    userRepository
                            .findByEmail(email)
                            .orElse(null);

            if (user != null) {

                id = user.getUserId();

                fullName = user.getFullName();
            }

        } else if ("ROLE_PROPERTY_OWNER".equals(role)) {

            PropertyOwner owner =
                    propertyOwnerRepository
                            .findByEmail(email)
                            .orElse(null);

            if (owner != null) {

                id = owner.getOwnerId();

                fullName = owner.getFullName();
            }

        } else if ("ROLE_ADMIN".equals(role)) {

            Admin admin =
                    adminRepository
                            .findByEmail(email)
                            .orElse(null);

            if (admin != null) {

                id = admin.getAdminId();

                fullName = admin.getFullName();
            }
        }

        String deviceType =
                request.getDeviceType();

        if (deviceType == null ||
                deviceType.isEmpty()) {

            deviceType = "WEB";
        }

        return jwtUtil.generateToken(
                userDetails.getUsername(),
                fullName,
                role,
                deviceType,
                id
        );
    }

    @Override
    public void logout(String token) {

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        tokenBlacklist.add(token);
        System.out.println("User logged out, token blacklisted: " + token);
    }

    public static boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    public Object updateUser(Long id, RegisterRequestDTO dto) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getMobileNumber() != null) user.setMobileNumber(dto.getMobileNumber());
        if (dto.getEmail() != null) {
            if (!dto.getEmail().equals(user.getEmail()) && isEmailAlreadyUsed(dto.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(dto.getEmail());
        }        if (dto.getPassword() != null) user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        return user;
    }

    public Object updateOwner(Long id, RegisterRequestDTO dto) {
        PropertyOwner propertyOwner = propertyOwerRepository.findById(id).orElse(null);
        if (propertyOwner == null) return null;
        if (dto.getFullName() != null) propertyOwner.setFullName(dto.getFullName());
        if (dto.getMobileNumber() != null) propertyOwner.setMobileNumber(dto.getMobileNumber());
        if (dto.getEmail() != null) {
            if (!dto.getEmail().equals(propertyOwner.getEmail()) && isEmailAlreadyUsed(dto.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            propertyOwner.setEmail(dto.getEmail());
        }        if (dto.getPassword() != null) propertyOwner.setPassword(passwordEncoder.encode(dto.getPassword()));
        propertyOwerRepository.save(propertyOwner);
        return propertyOwner;
    }

    public Object updateAdmin(Long id, RegisterRequestDTO dto) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if (admin == null) return null;
        if (dto.getFullName() != null) admin.setFullName(dto.getFullName());
        if (dto.getMobileNumber() != null) admin.setMobileNumber(dto.getMobileNumber());
        if (dto.getEmail() != null) {
            if (!dto.getEmail().equals(admin.getEmail()) && isEmailAlreadyUsed(dto.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            admin.setEmail(dto.getEmail());
        }        if (dto.getPassword() != null) admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        adminRepository.save(admin);
        return admin;
    }

    public boolean deactivateUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return false;
        user.setIsActive("false");
        userRepository.save(user);
        return true;
    }

    public boolean deactivateOwner(Long id) {
        PropertyOwner propertyOwner = propertyOwerRepository.findById(id).orElse(null);
        if (propertyOwner == null) return false;
        propertyOwner.setIsActive("false");
        propertyOwerRepository.save(propertyOwner);
        return true;
    }

    public boolean deactivateAdmin(Long id) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if (admin == null) return false;
        admin.setIsActive("false");
        adminRepository.save(admin);
        return true;
    }

    private boolean isEmailAlreadyUsed(String email) {
        return userRepository.existsByEmail(email)
                || propertyOwerRepository.existsByEmail(email)
                || adminRepository.existsByEmail(email);
    }

    @Override
    public void sendForgotPasswordOtp(ForgotPasswordRequestDTO dto) {

        User user = userRepository.findByEmail(dto.getEmail()).orElse(null);

        if (user == null) {

            PropertyOwner owner =
                    propertyOwnerRepository.findByEmail(dto.getEmail()).orElse(null);

            if (owner == null) {

                Admin admin =
                        adminRepository.findByEmail(dto.getEmail()).orElse(null);

                if (admin == null) {
                    throw new RuntimeException("Email not found");
                }
            }
        }

        ForgotPasswordOtp existing =
                forgotPasswordOtpRepository.findByEmail(dto.getEmail());

        // RESEND OTP AFTER 2 MINUTES
        if (existing != null) {

            LocalDateTime lastSentTime =
                    existing.getExpiryTime().minusMinutes(5);

            LocalDateTime nextAllowedTime =
                    lastSentTime.plusMinutes(2);

            if (LocalDateTime.now().isBefore(nextAllowedTime)) {

                long seconds =
                        java.time.Duration.between(
                                LocalDateTime.now(),
                                nextAllowedTime
                        ).getSeconds();

                throw new RuntimeException(
                        "Please wait " + seconds +
                                " seconds before requesting another OTP"
                );
            }
        }

        String otp = String.valueOf(
                (int) ((Math.random() * 900000) + 100000));

        if (existing != null) {

            existing.setOtp(otp);
            existing.setVerified(false);
            existing.setExpiryTime(LocalDateTime.now().plusMinutes(5));

            forgotPasswordOtpRepository.save(existing);

        } else {

            ForgotPasswordOtp forgotPasswordOtp =
                    new ForgotPasswordOtp();

            forgotPasswordOtp.setEmail(dto.getEmail());
            forgotPasswordOtp.setOtp(otp);
            forgotPasswordOtp.setVerified(false);
            forgotPasswordOtp.setExpiryTime(
                    LocalDateTime.now().plusMinutes(5));

            forgotPasswordOtpRepository.save(forgotPasswordOtp);
        }

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(dto.getEmail());

        message.setSubject("Forgot Password OTP");

        message.setText(
                "Hello User,\n\n" +

                        "We received a request to reset your password.\n\n" +

                        "Your One-Time Password (OTP) is: " + otp + "\n\n" +

                        "This OTP is valid for 5 minutes. Please do not share it with anyone for security reasons.\n\n" +

                        "If you did not request a password reset, please ignore this email.\n\n" +

                        "Thank you,\n" +

                        "Support Team"
        );

        javaMailSender.send(message);
    }

    @Override
    public boolean verifyOtp(VerifyOtpDTO dto) {

        ForgotPasswordOtp forgotPasswordOtp =
                forgotPasswordOtpRepository.findByEmail(dto.getEmail());

        if (forgotPasswordOtp == null) {
            return false;
        }

        if (!forgotPasswordOtp.getOtp().equals(dto.getOtp())) {
            return false;
        }

        if (forgotPasswordOtp.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            return false;
        }

        forgotPasswordOtp.setVerified(true);

        forgotPasswordOtpRepository.save(forgotPasswordOtp);

        return true;
    }

    @Override
    public boolean resetPassword(ResetPasswordDTO dto) {

        ForgotPasswordOtp forgotPasswordOtp =
                forgotPasswordOtpRepository.findByEmail(dto.getEmail());

        if (forgotPasswordOtp == null) {
            return false;
        }

        if (!forgotPasswordOtp.getVerified()) {
            return false;
        }

        if (!dto.getNewPassword()
                .equals(dto.getConfirmPassword())) {

            return false;
        }

        String encodedPassword =
                passwordEncoder.encode(dto.getNewPassword());

        User user =
                userRepository.findByEmail(dto.getEmail()).orElse(null);

        if (user != null) {

            user.setPassword(encodedPassword);

            userRepository.save(user);

            return true;
        }

        PropertyOwner owner =
                propertyOwnerRepository.findByEmail(dto.getEmail()).orElse(null);

        if (owner != null) {

            owner.setPassword(encodedPassword);

            propertyOwnerRepository.save(owner);

            return true;
        }

        Admin admin =
                adminRepository.findByEmail(dto.getEmail()).orElse(null);

        if (admin != null) {

            admin.setPassword(encodedPassword);

            adminRepository.save(admin);

            return true;
        }

        return false;
    }

    @Override
    public void sendEmailVerificationOtp(
            SendEmailOtpDTO dto) {

        String email =
                dto.getEmail().toLowerCase().trim();

        // CHECK EXISTING EMAIL
        boolean userExists =
                userRepository.existsByEmail(email);

        boolean ownerExists =
                propertyOwnerRepository.existsByEmail(email);

        boolean adminExists =
                adminRepository.existsByEmail(email);

        if (userExists || ownerExists || adminExists) {

            throw new RuntimeException(
                    "Email already exists");
        }

        // CHECK EXISTING OTP
        EmailVerificationOtp existing =
                emailVerificationOtpRepository
                        .findByEmail(email);

        // RESEND OTP AFTER 2 MINUTES
        if (existing != null) {

            LocalDateTime lastSentTime =
                    existing.getExpiryTime().minusMinutes(5);

            LocalDateTime nextAllowedTime =
                    lastSentTime.plusMinutes(2);

            if (LocalDateTime.now().isBefore(nextAllowedTime)) {

                long seconds =
                        java.time.Duration.between(
                                LocalDateTime.now(),
                                nextAllowedTime
                        ).getSeconds();

                throw new RuntimeException(
                        "Please wait " + seconds +
                                " seconds before requesting another OTP"
                );
            }
        }

        // GENERATE OTP
        String otp =
                String.valueOf(
                        (int)((Math.random() * 900000) + 100000)
                );

        // UPDATE EXISTING OTP
        if (existing != null) {

            existing.setOtp(otp);

            existing.setVerified(false);

            existing.setExpiryTime(
                    LocalDateTime.now().plusMinutes(5)
            );

            emailVerificationOtpRepository.save(existing);

        } else {

            // SAVE NEW OTP
            EmailVerificationOtp token =
                    new EmailVerificationOtp();

            token.setEmail(email);

            token.setOtp(otp);

            token.setVerified(false);

            token.setExpiryTime(
                    LocalDateTime.now().plusMinutes(5)
            );

            emailVerificationOtpRepository.save(token);
        }

        // SEND MAIL
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("support@rentalchaavi.com");
        message.setTo(email);
        message.setSubject("Email Verification OTP");
        message.setText(
                       "Hello User,\n\n" +
                        "Your email verification OTP is: " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n\n" +
                        "Please do not share this OTP with anyone.\n\n" +
                        "Thank you,\n" +
                        "Support Team");
        javaMailSender.send(message);
    }

    @Override
    public boolean verifyEmailOtp(
            VerifyEmailOtpDTO dto) {

        EmailVerificationOtp token =
                emailVerificationOtpRepository
                        .findByEmail(dto.getEmail());

        if (token == null) {
            return false;
        }

        if (!token.getOtp().equals(dto.getOtp())) {
            return false;
        }

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        token.setVerified(true);

        emailVerificationOtpRepository.save(token);

        return true;
    }
}

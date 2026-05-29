package com.caryanam.caryanam_broker.repository;


import com.caryanam.caryanam_broker.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationOtpRepository
        extends JpaRepository<EmailVerificationOtp, Long> {

    EmailVerificationOtp findByEmail(String email);

    void deleteByEmail(String email);
}
package com.caryanam.caryanam_broker.repository;


import com.caryanam.caryanam_broker.entity.ForgotPasswordOtp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForgotPasswordOtpRepository
        extends JpaRepository<ForgotPasswordOtp, Long> {

    ForgotPasswordOtp findByEmail(String email);
}
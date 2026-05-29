package com.caryanam.caryanam_broker.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "forgot_password_otp")
public class ForgotPasswordOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String otp;

    private Boolean verified = false;

    private LocalDateTime expiryTime;
}
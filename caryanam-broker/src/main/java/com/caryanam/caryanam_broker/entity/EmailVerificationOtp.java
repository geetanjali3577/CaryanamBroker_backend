package com.caryanam.caryanam_broker.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class EmailVerificationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String otp;

    private boolean verified;

    private LocalDateTime expiryTime;
    private LocalDateTime createdTime;


}
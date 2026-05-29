package com.caryanam.caryanam_broker.dto;


import lombok.Data;

@Data
public class VerifyEmailOtpDTO {

    private String email;

    private String otp;
}
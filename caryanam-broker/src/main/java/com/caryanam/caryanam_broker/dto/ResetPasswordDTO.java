package com.caryanam.caryanam_broker.dto;



import lombok.Data;

@Data
public class ResetPasswordDTO {

    private String email;

    private String newPassword;

    private String confirmPassword;
}
package com.caryanam.caryanam_broker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private int status;
    private String message;
    private String token;
}

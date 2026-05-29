package com.caryanam.caryanam_broker.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private String role;
}
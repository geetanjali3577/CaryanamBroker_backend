package com.caryanam.caryanam_broker.dto;


import com.caryanam.caryanam_broker.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class RegisterRequestDTO {


    private String fullName;
    private String mobileNumber;
    private String email;
    private String password;
    private Role role;
    private String isActive;
}




package com.caryanam.caryanam_broker.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {

    private String status;
    private String message;
    private T data;

}

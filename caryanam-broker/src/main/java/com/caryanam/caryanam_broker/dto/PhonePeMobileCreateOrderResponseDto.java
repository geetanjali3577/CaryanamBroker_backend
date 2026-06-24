package com.caryanam.caryanam_broker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhonePeMobileCreateOrderResponseDto {

    private String merchantOrderId;
    private String orderId;
    private String token;
    private String merchantId;
    private String environment;
}

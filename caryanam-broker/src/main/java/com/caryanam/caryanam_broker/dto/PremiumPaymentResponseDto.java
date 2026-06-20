package com.caryanam.caryanam_broker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PremiumPaymentResponseDto {

    private String orderId;

    private Double amount;

    private String paymentUrl;

    private String status;
}

package com.caryanam.caryanam_broker.service;

import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderRequestDto;
import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderResponseDto;
import com.caryanam.caryanam_broker.dto.PremiumPaymentResponseDto;

public interface PhonePeService {

    PremiumPaymentResponseDto createPremiumOrder(Long userId);
    String getAccessToken();
    void paymentSuccess(String orderId,
                        String transactionId);
    void propertyPaymentSuccess(
            String orderId,
            String transactionId);

    // Mobile SDK methods
    PhonePeMobileCreateOrderResponseDto createMobileOrder(PhonePeMobileCreateOrderRequestDto requestDto);
    Object verifyMobilePayment(String merchantOrderId);
}


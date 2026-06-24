package com.caryanam.caryanam_broker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhonePeMobileCreateOrderRequestDto {

    private Long userId;
    private Long ownerId;
    private Long propertyId;
    private String premiumFor; // USER or OWNER
    private Long amount;
}

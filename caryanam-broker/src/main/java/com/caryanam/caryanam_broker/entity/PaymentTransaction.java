package com.caryanam.caryanam_broker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long propertyId;
    private Long ownerId;



    private String paymentType;

    private String orderId;

    private String transactionId;

    private Double amount;

    private String paymentStatus;



    private LocalDateTime createdAt;
}

package com.caryanam.caryanam_broker.entity;

import com.caryanam.caryanam_broker.Enum.PaymentStatus;
import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "base_amount")
    private Double baseAmount;

    @Column(name = "gst_amount")
    private Double gstAmount;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "payment_response", columnDefinition = "TEXT")
    private String paymentResponse;

    @Column(name = "created_by")
    private String createdBy;

    private LocalDateTime createdAt;
}

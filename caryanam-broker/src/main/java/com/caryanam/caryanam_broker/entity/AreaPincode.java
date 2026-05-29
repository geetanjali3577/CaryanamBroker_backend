package com.caryanam.caryanam_broker.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class AreaPincode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    private String area;

    private String pincode;

    @Column(
            name = "near_by",
            columnDefinition = "TEXT"
    )
    private String nearBy;

    private String nearbyPincode;
}
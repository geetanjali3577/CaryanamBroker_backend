package com.caryanam.caryanam_broker.entity;

import com.caryanam.caryanam_broker.enums.FacilityName;
import com.caryanam.caryanam_broker.enums.FacilityStatus;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "owner_facilities")
public class OwnerFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    private Long propertyId;

    @Enumerated(EnumType.STRING)
    private FacilityName facilityName;

    @Enumerated(EnumType.STRING)
    private FacilityStatus status;


}
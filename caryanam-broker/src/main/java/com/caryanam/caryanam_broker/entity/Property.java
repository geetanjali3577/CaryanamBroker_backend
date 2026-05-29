package com.caryanam.caryanam_broker.entity;
import com.caryanam.caryanam_broker.Enum.BhkType;
import com.caryanam.caryanam_broker.Enum.FurnishingType;
import com.caryanam.caryanam_broker.Enum.PgType;
import com.caryanam.caryanam_broker.Enum.PropertyType;
import jakarta.persistence.*;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private Double  price;
    private String location;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String mobileNumber;

    private Integer likesCount;
    private Integer viewsCount;
    private String status;

    private String city;
    private String address;
    private String state;
    private String pincode;

    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;
    @Enumerated(EnumType.STRING)
    private PgType pgType;
    @Enumerated(EnumType.STRING)
    private BhkType bhkType;
    @Enumerated(EnumType.STRING)
    private FurnishingType furnishing;
    private String carpetArea;
    private String coverImage;
    private String doctypeImages;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private PropertyOwner propertyOwner;
    private boolean isPremiumActive;
    private String paymentStatus;
    @Column(name = "apartment_name")
    private String apartmentName;
    private String nearBy;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
   }
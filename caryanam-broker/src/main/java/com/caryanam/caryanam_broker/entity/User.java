package com.caryanam.caryanam_broker.entity;



import com.caryanam.caryanam_broker.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;
    private String mobileNumber;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;
    private Integer premiumCount = 0;
    @Column(nullable = false)
    private Integer propertyLimit = 1;
    @Column(nullable = false)
    private boolean premiumActive = false;

//    @Column(nullable = false)
//    private String premiumStatus = "NONE";
@Column(columnDefinition = "TEXT")
private String premiumStatus = "";
    private String isActive;
}
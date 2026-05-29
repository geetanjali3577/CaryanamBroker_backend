//package com.caryanam.caryanam_broker.entity;
//
//import com.caryanam.caryanam_broker.enums.Role;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.util.List;
//
//
//@Data
//
//@Entity
//@Table(name = "admin")
//public class Admin {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long adminId;
//
//    private String fullName;
//    private String mobileNumber;
//
//    @Column(unique = true, nullable = false)
//    private String email;
//
//    private String password;
//
//    private Integer premiumCount=0;
//
//
//    @Enumerated(EnumType.STRING)
//    private Role role;
//
//    @Column(nullable = false)
//    private Integer propertyLimit = 1;
//
//    @Column(nullable = false)
//    private boolean premiumActive = false;
//
//    @Column(nullable = false)
//    private String premiumStatus = "NONE";
//}

package com.caryanam.caryanam_broker.entity;

import com.caryanam.caryanam_broker.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminId;

    private String fullName;
    private String mobileNumber;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;
    private String isActive;
}
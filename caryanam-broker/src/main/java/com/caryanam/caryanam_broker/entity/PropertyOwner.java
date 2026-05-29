//package com.caryanam.caryanam_broker.entity;
//import com.caryanam.caryanam_broker.enums.Role;
//import jakarta.persistence.*;
//import lombok.Data;
//
//
//@Data
//@Entity
//@Table(name = "P_Owner")
//public class PropertyOwner {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long ownerId;
//
//    private String fullName;
//    private String mobileNumber;
//    private String password;
//
//    @Column(unique = true, nullable = false)
//    private String email;
//
//    @Enumerated(EnumType.STRING)
//    private Role role;
//
//}
//


package com.caryanam.caryanam_broker.entity;

import com.caryanam.caryanam_broker.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "P_Owner")
public class PropertyOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ownerId;
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
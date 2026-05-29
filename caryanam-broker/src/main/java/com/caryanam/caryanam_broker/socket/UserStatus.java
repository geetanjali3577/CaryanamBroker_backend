package com.caryanam.caryanam_broker.socket;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "user_status")
public class UserStatus {

    @Id
    private Long userId;
    private boolean online;
}

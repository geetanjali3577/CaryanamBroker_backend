package com.caryanam.caryanam_broker.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "property_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "property_id"})
        })
public class PropertyLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "property_id")
    private Property property;

    private LocalDateTime likedAt = LocalDateTime.now();
}

package com.caryanam.caryanam_broker.repository;

import com.caryanam.caryanam_broker.entity.PropertyOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyOwnerRepository extends JpaRepository<PropertyOwner, Long> {

    boolean existsByEmail(String email);

    Optional<PropertyOwner> findByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);
}

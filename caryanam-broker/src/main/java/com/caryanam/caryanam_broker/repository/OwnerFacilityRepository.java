package com.caryanam.caryanam_broker.repository;

import com.caryanam.caryanam_broker.entity.OwnerFacility;
import com.caryanam.caryanam_broker.enums.FacilityName;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerFacilityRepository extends JpaRepository<OwnerFacility, Long> {

    List<OwnerFacility> findByOwnerIdAndPropertyId(Long ownerId, Long propertyId);

    Optional<OwnerFacility> findByOwnerIdAndPropertyIdAndFacilityName(
            Long ownerId, Long propertyId, FacilityName facilityName);


}
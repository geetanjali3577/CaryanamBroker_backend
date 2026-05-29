package com.caryanam.caryanam_broker.repository;



import com.caryanam.caryanam_broker.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    int countByPropertyId(Long propertyId);

    List<PropertyImage> findByPropertyId(Long propertyId);

    Optional<PropertyImage> findFirstByImagePath(String imagePath);
}
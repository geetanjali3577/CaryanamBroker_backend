package com.caryanam.caryanam_broker.repository;

import com.caryanam.caryanam_broker.entity.AreaPincode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaPincodeRepository extends JpaRepository<AreaPincode, Long> {

    List<AreaPincode> findByCityIgnoreCase(String city);

    AreaPincode findByCityIgnoreCaseAndAreaIgnoreCase(String city, String area);

    List<AreaPincode> findByNearbyPincode(String nearbyPincode);

    List<AreaPincode> findByPincode(String pincode);

}
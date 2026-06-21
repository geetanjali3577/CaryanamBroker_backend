package com.caryanam.caryanam_broker.repository;

import com.caryanam.caryanam_broker.entity.Property;
import com.caryanam.caryanam_broker.enums.PremiumStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByStatus(String active);

    List<Property> findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Property> findByCityIgnoreCaseAndAddressIgnoreCaseAndStatus(String city, String address, String active);

    List<Property> findByCityIgnoreCaseAndStatus(String city, String active);

    Optional<Property> findByPaymentOrderId(String paymentOrderId);

    List<Property> findByPremiumStatus(PremiumStatus status);

    List<Property> findByPremiumEndDateBeforeAndPremiumStatusIn(LocalDateTime dateTime, List<PremiumStatus> statuses);
}

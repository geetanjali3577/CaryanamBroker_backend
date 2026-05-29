package com.caryanam.caryanam_broker.repository;

import com.caryanam.caryanam_broker.entity.PropertyLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyLikeRepository extends JpaRepository<PropertyLike, Long> {

    Optional<PropertyLike> findByUser_UserIdAndProperty_Id(Long userId, Long propertyId);

    List<PropertyLike> findByUser_UserId(Long userId);

    Integer countByProperty_Id(Long propertyId);

    void deleteByUser_UserIdAndProperty_Id(Long userId, Long propertyId);
}
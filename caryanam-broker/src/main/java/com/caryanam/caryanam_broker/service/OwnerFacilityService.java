package com.caryanam.caryanam_broker.service;

import com.caryanam.caryanam_broker.dto.OwnerFacilityRequest;
import com.caryanam.caryanam_broker.entity.OwnerFacility;

import java.util.List;

public interface OwnerFacilityService {

    String saveFacilities(
            OwnerFacilityRequest request);



    List<OwnerFacility> getFacilities(
            Long ownerId,
            Long propertyId);
}

package com.caryanam.caryanam_broker.serviceimpl;

import com.caryanam.caryanam_broker.dto.FacilityDto;
import com.caryanam.caryanam_broker.dto.OwnerFacilityRequest;
import com.caryanam.caryanam_broker.entity.OwnerFacility;
import com.caryanam.caryanam_broker.repository.OwnerFacilityRepository;
import com.caryanam.caryanam_broker.service.OwnerFacilityService;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OwnerFacilityServiceImpl
        implements OwnerFacilityService {

    @Autowired
    private OwnerFacilityRepository repository;

    @Transactional
    @Override
    public String saveFacilities(OwnerFacilityRequest request) {

        List<OwnerFacility> facilityList = new ArrayList<>();
        for (FacilityDto facilityDto : request.getFacilities()) {

            OwnerFacility ownerFacility = repository
                            .findByOwnerIdAndPropertyIdAndFacilityName(request.getOwnerId(), request.getPropertyId(), facilityDto.getFacilityName())
                            .orElse(new OwnerFacility());

            ownerFacility.setOwnerId(request.getOwnerId());
            ownerFacility.setPropertyId(request.getPropertyId());
            ownerFacility.setFacilityName(facilityDto.getFacilityName());
            ownerFacility.setStatus(facilityDto.getStatus());
            facilityList.add(ownerFacility);
        }

        repository.saveAll(facilityList);

        return "Facilities Saved Successfully";
    }



    @Override
    public List<OwnerFacility> getFacilities(Long ownerId, Long propertyId) {

        return repository.findByOwnerIdAndPropertyId(ownerId, propertyId);
    }
}
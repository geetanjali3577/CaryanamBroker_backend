package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.dto.OwnerFacilityRequest;
import com.caryanam.caryanam_broker.dto.ResponseHandler;
import com.caryanam.caryanam_broker.entity.OwnerFacility;
import com.caryanam.caryanam_broker.entity.PropertyOwner;
import com.caryanam.caryanam_broker.messageconfig.MessageConfig;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.service.OwnerFacilityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@CrossOrigin("*")
public class OwnerFacilityController {

    @Autowired
    private OwnerFacilityService service;

    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;

    @PostMapping("/save-facilities")
    public ResponseEntity<Object> saveFacilities(
            @RequestBody OwnerFacilityRequest request) {

        if (request.getOwnerId() == null) {
            return ResponseHandler.generateResponse("Owner Id Is Required", HttpStatus.BAD_REQUEST, null);
        }
        if (request.getPropertyId() == null) {
            return ResponseHandler.generateResponse("Property Id Is Required", HttpStatus.BAD_REQUEST, null);
        }
        PropertyOwner owner = propertyOwnerRepository.findById(request.getOwnerId()).orElse(null);
        if (owner == null) {
            return ResponseHandler.generateResponse(MessageConfig.OWNER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }
        if (request.getFacilities() == null || request.getFacilities().isEmpty()) {
            return ResponseHandler.generateResponse("Facilities List Cannot Be Empty", HttpStatus.BAD_REQUEST, null);
        }

        service.saveFacilities(request);
        List<OwnerFacility> savedFacilities = service.getFacilities(request.getOwnerId(), request.getPropertyId());
        return ResponseHandler.generateResponse("Facilities Saved Successfully", HttpStatus.OK, savedFacilities);
    }

    @GetMapping("/get-facilities")
    public ResponseEntity<Object> getFacilities(
            @RequestParam Long ownerId,
            @RequestParam Long propertyId) {

        PropertyOwner owner = propertyOwnerRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return ResponseHandler.generateResponse(MessageConfig.OWNER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }

        List<OwnerFacility> facilities = service.getFacilities(ownerId, propertyId);
        return ResponseHandler.generateResponse("Facilities Fetched Successfully", HttpStatus.OK, facilities);
    }
}
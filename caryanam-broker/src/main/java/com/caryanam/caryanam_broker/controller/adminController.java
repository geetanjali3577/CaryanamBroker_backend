package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.dto.ResponseHandler;
import com.caryanam.caryanam_broker.entity.Property;
import com.caryanam.caryanam_broker.entity.PropertyOwner;
import com.caryanam.caryanam_broker.entity.User;
import com.caryanam.caryanam_broker.messageconfig.MessageConfig;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.PropertyRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class adminController {

    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    private String currentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String[] statuses = status.split(",");
        return statuses[statuses.length - 1].trim().toUpperCase();
    }


    @GetMapping("/pending-users")
    public List<Map<String, Object>> getPendingUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();
        for (User user : users) {
            if ("PENDING".equals(currentStatus(user.getPremiumStatus()))) {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user.getUserId());
                map.put("fullName", user.getFullName());
                map.put("email", user.getEmail());
                map.put("mobileNumber", user.getMobileNumber());
                map.put("premiumStatus", "PENDING");
                map.put("premiumCount", user.getPremiumCount());
                response.add(map);
            }
        }
        return response;
    }

    @GetMapping("/pending-owner")
    public List<Map<String, Object>> getPendingOwners() {

        List<PropertyOwner> owners =
                propertyOwnerRepository.findAll();

        List<Map<String, Object>> response =
                new ArrayList<>();

        for (PropertyOwner owner : owners) {

            // OWNER PROPERTIES
            List<Property> properties =
                    propertyRepository.findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(
                            owner.getOwnerId()
                    );

            for (Property property : properties) {

                // ONLY PENDING PROPERTY
//                if ("PENDING".equalsIgnoreCase(
//                        property.getPaymentStatus())) {
                if ("SUCCESS".equalsIgnoreCase(
                        property.getPaymentStatus())) {

                    Map<String, Object> map =
                            new HashMap<>();

                    // OWNER DETAILS
                    map.put("ownerId",
                            owner.getOwnerId());

                    map.put("fullName",
                            owner.getFullName());

                    map.put("email",
                            owner.getEmail());

                    map.put("mobileNumber",
                            owner.getMobileNumber());

                    // PROPERTY DETAILS
                    map.put("propertyId",
                            property.getId());

                    map.put("title",
                            property.getTitle());

                    map.put("price",
                            property.getPrice());

                    map.put("city",
                            property.getCity());

                    map.put("location",
                            property.getLocation());

                    map.put("propertyType",
                            property.getPropertyType());

                    map.put("paymentStatus",
                            property.getPaymentStatus());

                    map.put("premiumActive",
                            property.isPremiumActive());

                    response.add(map);
                }
            }
        }

        return response;
    }

    @PostMapping("/approveOwnerPremium/{ownerId}")
    public ResponseEntity<Object> approveOwner(@PathVariable Long ownerId) {

        if (ownerId == null || ownerId <= 0) {
            return ResponseHandler.generateResponse(MessageConfig.INVALID_ID, HttpStatus.BAD_REQUEST, null);
        }

        PropertyOwner owner = propertyOwnerRepository.findById(ownerId).orElse(null);

        if (owner == null) {
            return ResponseHandler.generateResponse(MessageConfig.OWNER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }

        String status = owner.getPremiumStatus();

        if (status == null || !status.contains("PENDING")) {

            return ResponseHandler.generateResponse(
                    "No pending premium request found",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        // OWNER STATUS UPDATE
        status = status.replaceFirst("PENDING", "APPROVED");

        owner.setPremiumStatus(status);
        owner.setPremiumStatus(status);
        owner.setPremiumActive(true);

        propertyOwnerRepository.save(owner);

        // PROPERTY TABLE UPDATE
        List<Property> properties =
                propertyRepository.findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(ownerId);

        for (Property property : properties) {

            property.setPaymentStatus("APPROVED");
            property.setPremiumActive(true);

            propertyRepository.save(property);
        }

        return ResponseHandler.generateResponse(
                MessageConfig.OWNER_PREMIUM_APPROVED,
                HttpStatus.OK,
                owner
        );
    }

    @PostMapping("/approveUserPremium/{userId}")
    public ResponseEntity<Object> approveUser(@PathVariable Long userId) {

        if (userId == null || userId <= 0) {
            return ResponseHandler.generateResponse(MessageConfig.INVALID_ID, HttpStatus.BAD_REQUEST, null);
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseHandler.generateResponse(MessageConfig.USER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }

        String status = user.getPremiumStatus();

        if (!"PENDING".equals(currentStatus(status))) {

            return ResponseHandler.generateResponse("No pending premium request found", HttpStatus.BAD_REQUEST, null);
        }
        if(!"SUCCESS".equals(user.getPaymentStatus())){

            return ResponseHandler.generateResponse(
                    "Payment not completed",
                    HttpStatus.BAD_REQUEST,
                    null);
        }
        user.setPremiumStatus("APPROVED");
        user.setPremiumActive(true);

        userRepository.save(user);

        return ResponseHandler.generateResponse(MessageConfig.USER_PREMIUM_APPROVED, HttpStatus.OK, user);
    }

    @PostMapping("/rejectOwnerPremium/{ownerId}")
    public ResponseEntity<Object> rejectOwner(@PathVariable Long ownerId) {

        if (ownerId == null || ownerId <= 0) {
            return ResponseHandler.generateResponse(
                    MessageConfig.INVALID_ID,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        PropertyOwner owner =
                propertyOwnerRepository.findById(ownerId).orElse(null);

        if (owner == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.OWNER_NOT_FOUND,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        if (!owner.getPremiumStatus().contains("PENDING")) {

            return ResponseHandler.generateResponse(
                    "Owner has not requested premium or already processed",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        // OWNER STATUS UPDATE
        owner.setPremiumStatus("REJECTED");
        owner.setPremiumActive(false);

        propertyOwnerRepository.save(owner);

        // PROPERTY TABLE UPDATE
        List<Property> properties =
                propertyRepository.findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(ownerId);

        for (Property property : properties) {

            property.setPaymentStatus("REJECTED");
            property.setPremiumActive(false);

            propertyRepository.save(property);
        }

        return ResponseHandler.generateResponse(
                MessageConfig.OWNER_PREMIUM_REJECTED,
                HttpStatus.OK,
                null
        );
    }

    @PostMapping("/rejectUserPremium/{userId}")
    public ResponseEntity<Object> rejectUser(@PathVariable Long userId) {

        if (userId == null || userId <= 0) {
            return ResponseHandler.generateResponse(MessageConfig.INVALID_ID, HttpStatus.BAD_REQUEST, null);
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseHandler.generateResponse(MessageConfig.USER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }


        if (!"PENDING".equals(currentStatus(user.getPremiumStatus()))) {
            return ResponseHandler.generateResponse("User has not requested premium or already processed", HttpStatus.BAD_REQUEST, null);
        }

        user.setPremiumStatus("REJECTED");
        user.setPremiumActive(false);

        userRepository.save(user);

        return ResponseHandler.generateResponse(MessageConfig.USER_PREMIUM_REJECTED, HttpStatus.OK, null);
    }

    @GetMapping("/owner/{ownerId}/properties")
    public ResponseEntity<?> getOwnerProperties(@PathVariable Long ownerId) {
        PropertyOwner owner = propertyOwnerRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return ResponseHandler.generateResponse(MessageConfig.OWNER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }
        List<Property> properties = propertyRepository.findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(ownerId);
        Map<String, Object> response = new HashMap<>();
        response.put("ownerId", ownerId);
        response.put("ownerName", owner.getFullName());
        response.put("premiumStatus", owner.getPremiumStatus());
        response.put("premiumActive", owner.isPremiumActive());
        response.put("totalProperties", properties.size());
        response.put("properties", properties);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approveProperty/{propertyId}")
    public ResponseEntity<Object> approveProperty(@PathVariable Long propertyId) {
        if (propertyId == null || propertyId <= 0) {
            return ResponseHandler.generateResponse(MessageConfig.INVALID_ID, HttpStatus.BAD_REQUEST, null);
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            return ResponseHandler.generateResponse("Property not found", HttpStatus.BAD_REQUEST, null);
        }

//        if (!"PENDING".equalsIgnoreCase(property.getPaymentStatus())) {
//            return ResponseHandler.generateResponse(
//                    "Property is not in pending status",
//                    HttpStatus.BAD_REQUEST,
//                    null
//            );
//        }
        if (!"SUCCESS".equalsIgnoreCase(property.getPaymentStatus())) {

            return ResponseHandler.generateResponse(
                    "Payment not completed",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        // UPDATE PROPERTY STATUS
        property.setPaymentStatus("APPROVED");
        property.setPremiumActive(true);
        propertyRepository.save(property);

        // UPDATE OWNER STATUS
        PropertyOwner owner = property.getPropertyOwner();
        if (owner != null) {
            String status = owner.getPremiumStatus();
            if (status == null || !status.contains("APPROVED")) {
                if (status == null || status.isEmpty()) {
                    owner.setPremiumStatus("APPROVED");
                } else {
                    owner.setPremiumStatus(status + ",APPROVED");
                }
                owner.setPremiumActive(true);
                propertyOwnerRepository.save(owner);
            }
        }

        return ResponseHandler.generateResponse(
                "Property approved successfully",
                HttpStatus.OK,
                property
        );
    }

    @PostMapping("/rejectProperty/{propertyId}")
    public ResponseEntity<Object> rejectProperty(@PathVariable Long propertyId) {
        if (propertyId == null || propertyId <= 0) {
            return ResponseHandler.generateResponse(MessageConfig.INVALID_ID, HttpStatus.BAD_REQUEST, null);
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            return ResponseHandler.generateResponse("Property not found", HttpStatus.BAD_REQUEST, null);
        }

        if (!"PENDING".equalsIgnoreCase(property.getPaymentStatus())) {
            return ResponseHandler.generateResponse(
                    "Property is not in pending status",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        // UPDATE PROPERTY STATUS
        property.setPaymentStatus("REJECTED");
        property.setPremiumActive(false);
        propertyRepository.save(property);

        return ResponseHandler.generateResponse(
                "Property rejected successfully",
                HttpStatus.OK,
                null
        );
    }
}
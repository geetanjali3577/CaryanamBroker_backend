package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.configuration.CustomUserDetails;
import com.caryanam.caryanam_broker.dto.PropertyDto;
import com.caryanam.caryanam_broker.dto.ResponseDto;
import com.caryanam.caryanam_broker.dto.ResponseHandler;
import com.caryanam.caryanam_broker.entity.*;
import com.caryanam.caryanam_broker.messageconfig.MessageConfig;
import com.caryanam.caryanam_broker.repository.*;
import com.caryanam.caryanam_broker.service.PropertyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
public class OwnerPropertyController {

    @Autowired private PropertyService propertyService;
    @Autowired private PropertyImageRepository propertyImageRepository;
    @Autowired private PropertyOwnerRepository propertyOwnerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PaymentTransactionRepository paymentRepo;
    @Autowired
    private AreaPincodeRepository areaPincodeRepository;

    // ================= COMMON METHODS =================

    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Long getLoggedInOwnerId() {
        Authentication auth = getAuth();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else if (principal instanceof String) {
            String email = (String) principal;
            PropertyOwner owner = propertyOwnerRepository.findByEmail(email).orElse(null);
            return owner != null ? owner.getOwnerId() : null;
        }
        return null;
    }

    private boolean isAdmin() {
        Authentication auth = getAuth();
        if (auth == null) return false;

        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/getAreasByCity/{city}")
    public ResponseEntity<Object> getAreasByCity(@PathVariable String city) {

        List<AreaPincode> list = areaPincodeRepository.findByCityIgnoreCase(city);
        List<String> areas = new ArrayList<>();
        for (AreaPincode area : list) {
            areas.add(area.getArea());
        }

        return ResponseHandler.generateResponse("Areas fetched successfully", HttpStatus.OK, areas);
    }

    // ================= GET PINCODE =================
    @GetMapping("/getPincode")
    public ResponseEntity<Object> getPincode(@RequestParam String city, @RequestParam String area) {
        AreaPincode data = areaPincodeRepository.findByCityIgnoreCaseAndAreaIgnoreCase(city, area);
        if (data == null) {
            return ResponseHandler.generateResponse("Area not found", HttpStatus.BAD_REQUEST, null);
        }
        return ResponseHandler.generateResponse("Pincode fetched successfully", HttpStatus.OK, data.getPincode());
    }

    @PostMapping("/addPropertyByOwner/{ownerId}")
    public ResponseEntity<Object> addProperty(
            @PathVariable Long ownerId, @RequestBody PropertyDto propertyDto) {
        Long loggedInOwnerId = getLoggedInOwnerId();
        if (loggedInOwnerId == null) {
            return ResponseEntity.status(401).body(new ResponseDto<>(401, MessageConfig.UNAUTHORIZED, null));
        }
        if (!isAdmin() && !loggedInOwnerId.equals(ownerId)) {
            return ResponseEntity.status(403).body(new ResponseDto<>(403, MessageConfig.FORBIDDEN, null));
        }

        if (propertyDto.getTitle() == null || propertyDto.getTitle().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Title is required", HttpStatus.BAD_REQUEST, null);
        }
        for (int i = 0; i < propertyDto.getTitle().length(); i++) {
            if (Character.isDigit(propertyDto.getTitle().charAt(i))) {
                return ResponseHandler.generateResponse("Title should not contain numbers", HttpStatus.BAD_REQUEST, null);
            }
        }
        String title = propertyDto.getTitle().trim();
        if (title.length() < 3 || title.length() > 50) {
            return ResponseHandler.generateResponse("Property title must be between 3 to 50 characters", HttpStatus.BAD_REQUEST, null);
        }
        if (!title.matches("^[A-Za-z ]+$")) {
            return ResponseHandler.generateResponse("Property title must contain only alphabets", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getPrice() == null) {
            return ResponseHandler.generateResponse("Price is required", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getPrice() <= 0) {
            return ResponseHandler.generateResponse("Price must be greater than 0", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getLocation() == null || propertyDto.getLocation().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Location is required", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getAddress() == null || propertyDto.getAddress().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Address is required", HttpStatus.BAD_REQUEST, null);
        }
        String address = propertyDto.getAddress().trim();
        if (address.length() < 10 || address.length() > 150) {
            return ResponseHandler.generateResponse(
                    "Address must be between 10 to 150 characters", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getCity() == null || propertyDto.getCity().trim().isEmpty()) {
            return ResponseHandler.generateResponse("City is required", HttpStatus.BAD_REQUEST, null);
        }
        if (!propertyDto.getCity().matches("^[A-Za-z ]+$")) {
            return ResponseHandler.generateResponse("City must contain only letters", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getState() == null || propertyDto.getState().trim().isEmpty()) {
            return ResponseHandler.generateResponse("State is required", HttpStatus.BAD_REQUEST, null);
        }
        if (!propertyDto.getState().matches("^[A-Za-z ]+$")) {
            return ResponseHandler.generateResponse("State must contain only letters", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getPincode() == null || propertyDto.getPincode().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Pincode is required", HttpStatus.BAD_REQUEST, null);
        }
        if (!propertyDto.getPincode().matches("\\d{6}")) {
            return ResponseHandler.generateResponse("Pincode must be 6 digits", HttpStatus.BAD_REQUEST, null);
        }
        AreaPincode areaData =
                areaPincodeRepository.findByCityIgnoreCaseAndAreaIgnoreCase(propertyDto.getCity(), propertyDto.getLocation());
        if (areaData == null) {
            return ResponseHandler.generateResponse("Invalid city or area", HttpStatus.BAD_REQUEST, null);
        }
        if (!areaData.getPincode().equals(propertyDto.getPincode())) {
            return ResponseHandler.generateResponse("Pincode does not match selected area", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getDescription() == null || propertyDto.getDescription().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Description is required", HttpStatus.BAD_REQUEST, null);
        }
        String description = propertyDto.getDescription().trim();
        if (description.length() < 40) {
            return ResponseHandler.generateResponse("Description must contain minimum 40 characters", HttpStatus.BAD_REQUEST, null);
        }
        if (description.length() > 7000) {
            return ResponseHandler.generateResponse("Description cannot exceed 7000 characters", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getPropertyType() == null) {
            return ResponseHandler.generateResponse("Property type is required", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getBhkType() == null) {
            return ResponseHandler.generateResponse("BHK type is required", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getFurnishing() == null) {
            return ResponseHandler.generateResponse("Furnishing is required", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getCarpetArea() == null || propertyDto.getCarpetArea().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Carpet area is required", HttpStatus.BAD_REQUEST, null);
        }
        String carpetArea = propertyDto.getCarpetArea().trim();
        if (!carpetArea.matches("^[0-9]+$")) {
            return ResponseHandler.generateResponse("Carpet area must contain only numbers", HttpStatus.BAD_REQUEST, null);
        }
        int area = Integer.parseInt(carpetArea);
        if (area <= 0) {
            return ResponseHandler.generateResponse("Carpet area must be greater than 0", HttpStatus.BAD_REQUEST, null);
        }
        if (area > 100000) {
            return ResponseHandler.generateResponse("Invalid carpet area", HttpStatus.BAD_REQUEST, null);
        }
        if (propertyDto.getMobileNumber() == null || propertyDto.getMobileNumber().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Mobile number is required", HttpStatus.BAD_REQUEST, null);
        }
        String mobile = propertyDto.getMobileNumber().trim();
        if (!mobile.matches("[6-9][0-9]{9}")) {
            return ResponseHandler.generateResponse("Mobile number must start with 6/7/8/9 and contain 10 digits", HttpStatus.BAD_REQUEST, null);
        }
        if (!propertyDto.getMobileNumber().matches("\\d{10}")) {
            return ResponseHandler.generateResponse("Mobile number must be 10 digits", HttpStatus.BAD_REQUEST, null);
        }

        if (propertyDto.getApartmentName() == null || propertyDto.getApartmentName().trim().isEmpty()) {
            return ResponseHandler.generateResponse("Apartment name is required", HttpStatus.BAD_REQUEST, null);
        }
        String apartmentName = propertyDto.getApartmentName().trim();
        if (apartmentName.length() < 3 || apartmentName.length() > 40) {
            return ResponseHandler.generateResponse(
                    "Apartment name must be between 3 to 40 characters",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }
        if (!apartmentName.matches("^[A-Za-z0-9 .,-]+$")) {
            return ResponseHandler.generateResponse(
                    "Apartment name contains invalid characters",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }
        PropertyOwner owner = propertyOwnerRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return ResponseHandler.generateResponse(MessageConfig.OWNER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }

        return ResponseHandler.generateResponse(MessageConfig.PROPERTY_ADDED, HttpStatus.OK, propertyService.addProperty(propertyDto, ownerId));
    }


    // ================= GET PROPERTY =================
    @GetMapping("/getPropertyById/{id}")
    public ResponseEntity<Object> getPropertyById(@PathVariable Long id) {
        Long loggedInOwnerId = getLoggedInOwnerId();
        if (loggedInOwnerId == null)
            return ResponseHandler.generateResponse(MessageConfig.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, null);
        Property property = propertyRepository.findById(id).orElse(null);
        if (property == null)
            return ResponseHandler.generateResponse(MessageConfig.PROPERTY_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        if (!isAdmin()) {
            if (property.getPropertyOwner() == null || !property.getPropertyOwner().getOwnerId().equals(loggedInOwnerId)) {
                return ResponseHandler.generateResponse(MessageConfig.FORBIDDEN, HttpStatus.FORBIDDEN, null);
            }
        }

        return ResponseHandler.generateResponse(MessageConfig.PROPERTY_FETCHED, HttpStatus.OK, propertyService.getPropertyById(id));
    }

    // ================= UPDATE =================
    @PutMapping("/updatePropertyById/{id}")
    public ResponseEntity<Object> updateProperty(@PathVariable Long id, @RequestBody PropertyDto propertyDto) {
        Long loggedInOwnerId = getLoggedInOwnerId();
        if (loggedInOwnerId == null)
            return ResponseHandler.generateResponse(MessageConfig.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, null);
        Property property = propertyRepository.findById(id).orElse(null);
        if (property == null)
            return ResponseHandler.generateResponse(MessageConfig.PROPERTY_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        if (!isAdmin()) {
            if (property.getPropertyOwner() == null || !property.getPropertyOwner().getOwnerId().equals(loggedInOwnerId)) {
                return ResponseHandler.generateResponse(MessageConfig.FORBIDDEN, HttpStatus.FORBIDDEN, null);
            }
        }
        return ResponseHandler.generateResponse(MessageConfig.PROPERTY_UPDATED, HttpStatus.OK, propertyService.updateProperty(id, propertyDto));
    }

    @DeleteMapping("/deletePropertyById/{id}")
    public ResponseEntity<Object> deleteProperty(@PathVariable Long id) {
        Long loggedInOwnerId = getLoggedInOwnerId();
        if (loggedInOwnerId == null)
            return ResponseHandler.generateResponse(MessageConfig.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, null);
        Property property = propertyRepository.findById(id).orElse(null);
        if (property == null)
            return ResponseHandler.generateResponse(MessageConfig.PROPERTY_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        if (!isAdmin()) {
            if (property.getPropertyOwner() == null || !property.getPropertyOwner().getOwnerId().equals(loggedInOwnerId)) {
                return ResponseHandler.generateResponse(MessageConfig.FORBIDDEN, HttpStatus.FORBIDDEN, null);
            }
        }
        return ResponseHandler.generateResponse(MessageConfig.PROPERTY_DELETED, HttpStatus.OK, propertyService.deleteProperty(id));
    }


    @PostMapping("/uploadPropertyImagesByPropertyId/{id}")
    public ResponseEntity<Object> uploadPropertyImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) {

        Long loggedInOwnerId = getLoggedInOwnerId();

        if (loggedInOwnerId == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED,
                    null
            );
        }

        // MAX IMAGE COUNT
        if (files.length > 10) {
            return ResponseHandler.generateResponse(
                    "Maximum 10 images allowed",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        // IMAGE VALIDATION
        for (MultipartFile file : files) {

            // SIZE VALIDATION
            if (file.getSize() > 5 * 1024 * 1024) {

                return ResponseHandler.generateResponse(
                        "Each image size should not exceed 5 MB",
                        HttpStatus.BAD_REQUEST,
                        null
                );
            }

            // TYPE VALIDATION
            String contentType = file.getContentType();

            if (contentType == null ||
                    !(contentType.equals("image/jpeg")
                            || contentType.equals("image/jpg")
                            || contentType.equals("image/png"))) {

                return ResponseHandler.generateResponse(
                        "Only JPG, JPEG and PNG images are allowed",
                        HttpStatus.BAD_REQUEST,
                        null
                );
            }
        }

        Property property =
                propertyRepository.findById(id).orElse(null);

        if (property == null) {

            return ResponseHandler.generateResponse(
                    MessageConfig.PROPERTY_NOT_FOUND,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        if (!isAdmin()) {

            if (property.getPropertyOwner() == null ||
                    !property.getPropertyOwner()
                            .getOwnerId()
                            .equals(loggedInOwnerId)) {

                return ResponseHandler.generateResponse(
                        MessageConfig.FORBIDDEN,
                        HttpStatus.FORBIDDEN,
                        null
                );
            }
        }

        return ResponseHandler.generateResponse(
                propertyService.uploadPropertyImages(id, files),
                HttpStatus.OK,
                null
        );
    }

//    @PostMapping("/buyPremiumByOwner/{ownerId}")
//    public ResponseEntity<Object> buyPremium(
//            @PathVariable Long ownerId,
//            @RequestParam(required = false) Long propertyId) {
//
//        PropertyOwner owner =
//                propertyOwnerRepository
//                        .findById(ownerId)
//                        .orElse(null);
//
//        if (owner == null) {
//
//            return ResponseHandler.generateResponse(
//                    MessageConfig.OWNER_NOT_FOUND,
//                    HttpStatus.BAD_REQUEST,
//                    null
//            );
//        }
//
//        // GET ALL OWNER PROPERTIES
//        List<Property> properties =
//                propertyRepository
//                        .findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(ownerId);
//
//        // IF PROPERTY ID IS PROVIDED, ONLY CHECK THAT PROPERTY
//        if (propertyId != null) {
//            Property specificProperty = propertyRepository.findById(propertyId).orElse(null);
//
//            if (specificProperty == null) {
//                return ResponseHandler.generateResponse(
//                        "Property not found",
//                        HttpStatus.BAD_REQUEST,
//                        null
//                );
//            }
//
//            // CHECK IF THIS SPECIFIC PROPERTY IS ALREADY APPROVED
//            if ("APPROVED".equalsIgnoreCase(specificProperty.getPaymentStatus())) {
//                return ResponseHandler.generateResponse(
//                        "Property already approved",
//                        HttpStatus.BAD_REQUEST,
//                        null
//                );
//            }
//
//            // SET PENDING FOR THIS SPECIFIC PROPERTY ONLY
//            specificProperty.setPaymentStatus("PENDING");
//            propertyRepository.save(specificProperty);
//
//            // UPDATE OWNER STATUS
//            String status = owner.getPremiumStatus();
//            if (status == null || status.isEmpty()) {
//                owner.setPremiumStatus("PENDING");
//            } else if (!status.contains("PENDING")) {
//                owner.setPremiumStatus(status + ",PENDING");
//            }
//            owner.setPremiumCount(owner.getPremiumCount() + 1);
//            propertyOwnerRepository.save(owner);
//
//            String qrUrl = "http://localhost:8080/qr/payment.png";
//            Map<String, Object> response = new HashMap<>();
//            response.put("message", MessageConfig.SCAN_QR);
//            response.put("qrCode", qrUrl);
//            response.put("propertyId", propertyId);
//
//            return ResponseHandler.generateResponse(
//                    MessageConfig.PAYMENT_INITIATED,
//                    HttpStatus.OK,
//                    response
//            );
//        }
//
//        // IF NO PROPERTY ID, CHECK ALL PROPERTIES (OLD BEHAVIOR)
//        for (Property property : properties) {
//            if ("APPROVED".equalsIgnoreCase(property.getPaymentStatus())) {
//                return ResponseHandler.generateResponse(
//                        "Premium already approved for property id : " + property.getId(),
//                        HttpStatus.BAD_REQUEST,
//                        null
//                );
//            }
//        }
//
//        // OWNER PREMIUM STATUS
//        String status = owner.getPremiumStatus();
//
//        if (status == null || status.isEmpty()) {
//            owner.setPremiumStatus("PENDING");
//        } else {
//            owner.setPremiumStatus(status + ",PENDING");
//        }
//
//        owner.setPremiumCount(owner.getPremiumCount() + 1);
//
//        // SET PENDING IN ALL PROPERTIES
//        for (Property property : properties) {
//            property.setPaymentStatus("PENDING");
//            propertyRepository.save(property);
//        }
//
//        propertyOwnerRepository.save(owner);
//
//        String qrUrl = "http://localhost:8080/qr/payment.png";
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", MessageConfig.SCAN_QR);
//        response.put("qrCode", qrUrl);
//        response.put("totalProperties", properties.size());
//
//        return ResponseHandler.generateResponse(
//                MessageConfig.PAYMENT_INITIATED,
//                HttpStatus.OK,
//                response
//        );
//    }
@PostMapping("/buyPremiumByOwner/{ownerId}")
public ResponseEntity<Object> buyPremium(
        @PathVariable Long ownerId,
        @RequestParam Long propertyId) {

    PropertyOwner owner = propertyOwnerRepository
            .findById(ownerId)
            .orElse(null);

    if (owner == null) {
        return ResponseHandler.generateResponse(
                MessageConfig.OWNER_NOT_FOUND,
                HttpStatus.BAD_REQUEST,
                null
        );
    }

    Property property = propertyRepository
            .findById(propertyId)
            .orElse(null);

    if (property == null) {
        return ResponseHandler.generateResponse(
                "Property not found",
                HttpStatus.BAD_REQUEST,
                null
        );
    }

    // PROPERTY BELONGS TO OWNER
    if (property.getPropertyOwner() == null ||
            !property.getPropertyOwner().getOwnerId().equals(ownerId)) {

        return ResponseHandler.generateResponse(
                "Property does not belong to owner",
                HttpStatus.BAD_REQUEST,
                null
        );
    }

    // ALREADY APPROVED
    if ("APPROVED".equalsIgnoreCase(property.getPaymentStatus())) {

        return ResponseHandler.generateResponse(
                "Property already approved",
                HttpStatus.BAD_REQUEST,
                null
        );
    }

    // PAYMENT ALREADY PENDING
    if ("PENDING".equalsIgnoreCase(property.getPaymentStatus())) {

        return ResponseHandler.generateResponse(
                "Payment already pending for this property",
                HttpStatus.BAD_REQUEST,
                null
        );
    }

    double amount = 116.82;

    String orderId =
            "PROPERTY_" + System.currentTimeMillis();

    // PROPERTY UPDATE
    property.setPaymentStatus("PENDING");
    property.setPaymentAmount(amount);
    property.setPaymentOrderId(orderId);

    propertyRepository.save(property);

    // PAYMENT TRANSACTION SAVE
    PaymentTransaction txn =
            new PaymentTransaction();

    txn.setOwnerId(ownerId);              // field asel tar
    txn.setPropertyId(propertyId);        // field asel tar
    txn.setOrderId(orderId);
    txn.setAmount(amount);
    txn.setPaymentStatus("PENDING");
    txn.setPaymentType("PROPERTY_PREMIUM");
    txn.setCreatedAt(LocalDateTime.now());

    paymentRepo.save(txn);

    Map<String, Object> response = new HashMap<>();

    response.put("propertyId", propertyId);
    response.put("orderId", orderId);
    response.put("amount", amount);
    response.put("paymentStatus", "PENDING");

    return ResponseHandler.generateResponse(
            MessageConfig.PAYMENT_INITIATED,
            HttpStatus.OK,
            response
    );
}

    @GetMapping("/getAllPropertiesByOwnerId/{ownerId}")
    public ResponseEntity<Object> getAllPropertiesByOwnerId(@PathVariable Long ownerId, HttpServletRequest request) {
        Long loggedInOwnerId = getLoggedInOwnerId();
        if (loggedInOwnerId == null) {
            return ResponseHandler.generateResponse(MessageConfig.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, null);
        }
        if (!isAdmin() && !loggedInOwnerId.equals(ownerId)) {
            return ResponseHandler.generateResponse(MessageConfig.FORBIDDEN, HttpStatus.FORBIDDEN, null);
        }
        PropertyOwner owner = propertyOwnerRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return ResponseHandler.generateResponse(MessageConfig.OWNER_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("properties", propertyService.getPropertiesByOwnerId(ownerId));
        response.put("premiumStatus", owner.getPremiumStatus());
        response.put("premiumActive", owner.isPremiumActive());
        return ResponseHandler.generateResponse(MessageConfig.PROPERTY_FETCHED, HttpStatus.OK, response);
    }
//Property.java
    @PutMapping("/activatePropertyById/{id}")
    public ResponseEntity<Object> activateProperty(@PathVariable Long id) {
        Long loggedInOwnerId = getLoggedInOwnerId();
        if (loggedInOwnerId == null) {
            return ResponseHandler.generateResponse(MessageConfig.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, null);
        }
        Property property = propertyRepository.findById(id).orElse(null);
        if (property == null) {
            return ResponseHandler.generateResponse(MessageConfig.PROPERTY_NOT_FOUND, HttpStatus.BAD_REQUEST, null);
        }
        if (!isAdmin()) {
            if (property.getPropertyOwner() == null || !property.getPropertyOwner().getOwnerId().equals(loggedInOwnerId)) {
                return ResponseHandler.generateResponse(MessageConfig.FORBIDDEN, HttpStatus.FORBIDDEN, null);
            }
        }
        return ResponseHandler.generateResponse("Property Activated Successfully", HttpStatus.OK, propertyService.activateProperty(id));
    }

    @GetMapping(value = "/property/image/{filename:.+}", produces = {
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            "image/gif"
    })
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        return propertyService.getPropertyImageContent(filename)
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(content.contentType()))
                        .body(content.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
//---------------------------------Payment Success------------------------//
@PostMapping("/property-payment-success")
public ResponseEntity<Object> paymentSuccess(
        @RequestParam String orderId,
        @RequestParam String transactionId) {

    Property property = propertyRepository
            .findByPaymentOrderId(orderId)
            .orElse(null);

    if (property == null) {
        return ResponseHandler.generateResponse(
                "Invalid Order Id",
                HttpStatus.BAD_REQUEST,
                null
        );
    }

    // PROPERTY UPDATE
    property.setPaymentStatus("SUCCESS");
    property.setPaymentTransactionId(transactionId);

    propertyRepository.save(property);

    // TRANSACTION UPDATE
    PaymentTransaction txn =
            paymentRepo.findByOrderId(orderId)
                    .orElse(null);

    if (txn != null) {
        txn.setTransactionId(transactionId);
        txn.setPaymentStatus("SUCCESS");

        paymentRepo.save(txn);
    }

    return ResponseHandler.generateResponse(
            "Payment Success",
            HttpStatus.OK,
            null
    );
}
}

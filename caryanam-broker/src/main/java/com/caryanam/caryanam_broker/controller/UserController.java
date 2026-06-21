package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.configuration.CustomUserDetails;
import com.caryanam.caryanam_broker.dto.PremiumPaymentResponseDto;
import com.caryanam.caryanam_broker.dto.PropertyDto;
import com.caryanam.caryanam_broker.dto.PropertyFilterDto;
import com.caryanam.caryanam_broker.dto.ResponseHandler;
import com.caryanam.caryanam_broker.entity.User;
import com.caryanam.caryanam_broker.messageconfig.MessageConfig;
import com.caryanam.caryanam_broker.repository.UserRepository;
import com.caryanam.caryanam_broker.service.PhonePeService;
import com.caryanam.caryanam_broker.service.PropertyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private PhonePeService phonePeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyService propertyService;

    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Long getLoggedInUserId() {
        Authentication authentication = getAuth();

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else if (principal instanceof String) {
            String email = (String) principal;
            User user = userRepository.findByEmail(email).orElse(null);
            return user != null ? user.getUserId() : null;
        }

        return null;
    }

    private boolean isAdmin() {
        Authentication auth = getAuth();

        if (auth == null) {
            return false;
        }

        return auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String currentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String[] statuses = status.split(",");
        return statuses[statuses.length - 1].trim().toUpperCase();
    }

    // ================= USER PREMIUM PAYMENT =================
    // POST /api/user/buyPremium/{userId}
    @PostMapping("/buyPremium/{userId}")
    public ResponseEntity<Object> buyPremium(@PathVariable Long userId) {

        if (userId == null || userId <= 0) {
            return ResponseHandler.generateResponse(
                    MessageConfig.INVALID_ID,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.USER_NOT_FOUND,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        // ================= SECURITY CHECK ADDED =================
        // Normal USER can buy premium only for own userId.
        // ADMIN can buy/initiate for any user.
        Long loggedInUserId = getLoggedInUserId();

        if (loggedInUserId == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED,
                    null
            );
        }

        if (!isAdmin() && !loggedInUserId.equals(userId)) {
            return ResponseHandler.generateResponse(
                    MessageConfig.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    null
            );
        }
        // ================= SECURITY CHECK END =================

        String currentPremiumStatus = currentStatus(user.getPremiumStatus());

        if ("APPROVED".equals(currentPremiumStatus) || user.isPremiumActive()) {
            return ResponseHandler.generateResponse(
                    "Premium already approved",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        if ("PENDING".equals(currentPremiumStatus)) {
            return ResponseHandler.generateResponse(
                    "Premium request already pending",
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        user.setPremiumStatus("PENDING");
        user.setPremiumActive(false);
        user.setPremiumCount(user.getPremiumCount() + 1);

        userRepository.save(user);

        PremiumPaymentResponseDto response = phonePeService.createPremiumOrder(userId);

        return ResponseHandler.generateResponse(
                "Payment Initiated",
                HttpStatus.OK,
                response
        );
    }

    // ================= GET USER PROPERTIES =================
    // GET /api/user/properties/{userId}
    @GetMapping("/properties/{userId}")
    public ResponseEntity<Object> getProperties(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {

        if (userId == null || userId <= 0) {
            return ResponseHandler.generateResponse(
                    MessageConfig.INVALID_ID,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        Long loggedInUserId = getLoggedInUserId();

        if (loggedInUserId == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED,
                    null
            );
        }

        if (!isAdmin() && !loggedInUserId.equals(userId)) {
            return ResponseHandler.generateResponse(
                    MessageConfig.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    null
            );
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.USER_NOT_FOUND,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        List<PropertyDto> data = propertyService.getAllProperties(userId, request);

        if (data.isEmpty()) {
            return ResponseHandler.generateResponse(
                    MessageConfig.NO_PROPERTIES_FOUND,
                    HttpStatus.OK,
                    data
            );
        }

        if (!user.isPremiumActive()) {
            return ResponseHandler.generateResponse(
                    MessageConfig.PREMIUM_REQUIRED,
                    HttpStatus.OK,
                    data
            );
        }

        return ResponseHandler.generateResponse(
                MessageConfig.PROPERTY_FETCHED,
                HttpStatus.OK,
                data
        );
    }

    // ================= FILTER USER PROPERTIES =================
    // POST /api/user/filter-properties/{userId}
    @PostMapping("/filter-properties/{userId}")
    public ResponseEntity<Object> filterProperties(
            @RequestBody PropertyFilterDto dto,
            @PathVariable Long userId
    ) {

        if (userId == null || userId <= 0) {
            return ResponseHandler.generateResponse(
                    MessageConfig.INVALID_ID,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        Long loggedInUserId = getLoggedInUserId();

        if (loggedInUserId == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED,
                    null
            );
        }

        if (!isAdmin() && !loggedInUserId.equals(userId)) {
            return ResponseHandler.generateResponse(
                    MessageConfig.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    null
            );
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.USER_NOT_FOUND,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        Object data = propertyService.filterProperties(dto, userId);

        if (data instanceof List<?> list
                && !list.isEmpty()
                && list.get(0) instanceof String) {

            return ResponseHandler.generateResponse(
                    "Areas fetched successfully",
                    HttpStatus.OK,
                    data
            );
        }

        List<PropertyDto> propertyList = (List<PropertyDto>) data;

        if (propertyList.isEmpty()) {
            return ResponseHandler.generateResponse(
                    MessageConfig.NO_PROPERTIES_FOUND,
                    HttpStatus.OK,
                    propertyList
            );
        }

        if (!user.isPremiumActive()) {
            return ResponseHandler.generateResponse(
                    MessageConfig.PREMIUM_REQUIRED,
                    HttpStatus.OK,
                    propertyList
            );
        }

        return ResponseHandler.generateResponse(
                MessageConfig.PROPERTY_FILTERED,
                HttpStatus.OK,
                propertyList
        );
    }

    // ================= GET USER BY ID =================
    // GET /api/user/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<Object> getUserById(@PathVariable Long userId) {

        if (userId == null || userId <= 0) {
            return ResponseHandler.generateResponse(
                    MessageConfig.INVALID_ID,
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        Long loggedInUserId = getLoggedInUserId();

        if (loggedInUserId == null) {
            return ResponseHandler.generateResponse(
                    MessageConfig.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED,
                    null
            );
        }

        if (!isAdmin() && !loggedInUserId.equals(userId)) {
            return ResponseHandler.generateResponse(
                    MessageConfig.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    null
            );
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseHandler.generateResponse(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    null
            );
        }

        Map<String, Object> response = new HashMap<>();

        response.put("id", user.getUserId());

        String premiumStatus = currentStatus(user.getPremiumStatus());

        response.put("premiumStatus", premiumStatus);
        response.put("premiumActive", user.isPremiumActive() || "APPROVED".equals(premiumStatus));

        return ResponseHandler.generateResponse(
                "User fetched successfully",
                HttpStatus.OK,
                response
        );
    }

    // ================= OLD MANUAL PAYMENT SUCCESS =================
    // POST /api/user/payment-success
    @PostMapping("/payment-success")
    public ResponseEntity<Object> paymentSuccess(
            @RequestParam String orderId,
            @RequestParam String transactionId
    ) {

        phonePeService.paymentSuccess(orderId, transactionId);

        return ResponseHandler.generateResponse(
                "Payment Success",
                HttpStatus.OK,
                null
        );
    }

    // ================= TEST PHONEPE TOKEN =================
    // GET /api/user/token
    @GetMapping("/token")
    public ResponseEntity<?> token() {
        return ResponseEntity.ok(phonePeService.getAccessToken());
    }
}
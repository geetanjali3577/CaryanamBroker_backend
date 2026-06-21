package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.Enum.PaymentStatus;
import com.caryanam.caryanam_broker.appconstant.AppConstants;
import com.caryanam.caryanam_broker.configuration.CustomUserDetails;
import com.caryanam.caryanam_broker.configuration.PhonePeConfig;
import com.caryanam.caryanam_broker.dto.PremiumPaymentResponseDto;
import com.caryanam.caryanam_broker.entity.PaymentTransaction;
import com.caryanam.caryanam_broker.entity.Property;
import com.caryanam.caryanam_broker.entity.PropertyOwner;
import com.caryanam.caryanam_broker.entity.User;
import com.caryanam.caryanam_broker.enums.PremiumStatus;
import com.caryanam.caryanam_broker.repository.PaymentTransactionRepository;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.PropertyRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class PremiumSubscriptionController {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;

    @Autowired
    private PaymentTransactionRepository paymentRepo;

    @Autowired
    private PhonePeConfig phonePeConfig;

    @Autowired
    private UserRepository userRepository;

    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Long getLoggedInOwnerId() {
        Authentication auth = getAuth();

        if (auth == null) {
            return null;
        }

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

    private String getLoggedInUsername() {
        Authentication auth = getAuth();

        if (auth == null) {
            return "anonymous";
        }

        return auth.getName();
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

    private String getFriendlyStatus(PremiumStatus status) {
        if (status == null) {
            return "None";
        }

        switch (status) {
            case FREE_ACTIVE:
            case ACTIVE:
                return "Premium Active";

            case PAYMENT_PENDING:
                return "Payment Pending";

            case PENDING_APPROVAL:
                return "Payment Success Waiting For Approval";

            case REJECTED:
                return "Premium Rejected";

            case EXPIRED:
                return "Premium Expired";

            default:
                return "None";
        }
    }

    // ================= OWNER PROPERTY PREMIUM PAYMENT =================
    // POST /premium/buy/{propertyId}
    @PostMapping("/premium/buy/{propertyId}")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER', 'ADMIN')")
    public ResponseEntity<Object> buyPremium(@PathVariable Long propertyId) {

        Long loggedInOwnerId = getLoggedInOwnerId();

        if (loggedInOwnerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);

        if (property == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Property not found"));
        }

        if (property.getPropertyOwner() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Property owner not found"));
        }

        if (!isAdmin() && !property.getPropertyOwner().getOwnerId().equals(loggedInOwnerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Forbidden: You do not own this property"));
        }

        if (property.getPremiumStatus() == PremiumStatus.ACTIVE
                || property.getPremiumStatus() == PremiumStatus.FREE_ACTIVE) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Property already has an active premium subscription"));
        }

        Optional<PaymentTransaction> existingPending =
                paymentRepo.findFirstByPropertyIdAndPaymentStatus(propertyId, PaymentStatus.PENDING);

        if (existingPending.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Payment already pending for this property"));
        }

        // ================= TEST AMOUNT ₹1 =================
        double baseAmount = 1.0;
        double gstAmount = 0.0;
        double totalAmount = 1.0;
        long phonePeAmount = 100L; // ₹1 = 100 paisa

        // ================= FINAL AMOUNT 99 + GST =================
        // double baseAmount = 99.0;
        // double gstAmount = 17.82;
        // double totalAmount = 116.82;
        // long phonePeAmount = Math.round(totalAmount * 100); // 11682 paisa

        String orderId = "PREMIUM_" + System.currentTimeMillis();

        PaymentTransaction txn = new PaymentTransaction();
        txn.setPropertyId(propertyId);
        txn.setOwnerId(property.getPropertyOwner().getOwnerId());
        txn.setOrderId(orderId);
        txn.setAmount(totalAmount);
        txn.setBaseAmount(baseAmount);
        txn.setGstAmount(gstAmount);
        txn.setTotalAmount(totalAmount);
        txn.setPaymentGateway("PhonePe");
        txn.setCreatedBy(getLoggedInUsername());
        txn.setPaymentStatus(PaymentStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(txn);

        property.setPremiumStatus(PremiumStatus.PAYMENT_PENDING);
        property.setPaymentOrderId(orderId);
        property.setPaymentStatus("PENDING");
        property.setPaymentAmount(totalAmount);
        propertyRepository.save(property);

        String paymentUrl;

        try {
            paymentUrl = createPhonePePaymentUrl(orderId, phonePeAmount, propertyId);
        } catch (Exception e) {
            e.printStackTrace();

            txn.setPaymentStatus(PaymentStatus.FAILED);
            txn.setPaymentResponse("PhonePe payment URL creation failed: " + e.getMessage());
            paymentRepo.save(txn);

            property.setPaymentStatus("FAILED");
            property.setPremiumStatus(PremiumStatus.REJECTED);
            propertyRepository.save(property);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "PhonePe payment URL creation failed",
                            "error", e.getMessage()
                    ));
        }

        PremiumPaymentResponseDto response = new PremiumPaymentResponseDto(
                orderId,
                totalAmount,
                paymentUrl,
                "PENDING"
        );

        return ResponseEntity.ok(response);
    }

    // ================= PHONEPE CREATE PAYMENT URL =================
    private String createPhonePePaymentUrl(String orderId, Long amountInPaisa, Long propertyId) {

        String accessToken = getAccessToken();

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "O-Bearer " + accessToken);

        Map<String, Object> merchantUrls = new HashMap<>();
        merchantUrls.put("redirectUrl", phonePeConfig.getRedirectUrl());

        Map<String, Object> paymentFlow = new HashMap<>();
        paymentFlow.put("type", "PG_CHECKOUT");
        paymentFlow.put("merchantUrls", merchantUrls);

        Map<String, Object> metaInfo = new HashMap<>();
        metaInfo.put("udf1", "PROPERTY_PREMIUM");
        metaInfo.put("udf2", String.valueOf(propertyId));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("merchantOrderId", orderId);
        requestBody.put("amount", amountInPaisa);
        requestBody.put("expireAfter", 1200);
        requestBody.put("paymentFlow", paymentFlow);
        requestBody.put("metaInfo", metaInfo);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                phonePeConfig.getPayUrl(),
                request,
                Map.class
        );

        Map body = response.getBody();

        if (body == null) {
            throw new RuntimeException("PhonePe response body is null");
        }

        Object redirectUrlObj = body.get("redirectUrl");

        if (redirectUrlObj == null) {
            throw new RuntimeException("PhonePe redirectUrl not received. Response: " + body);
        }

        return redirectUrlObj.toString();
    }

    // ================= PHONEPE AUTH TOKEN =================
    private String getAccessToken() {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", phonePeConfig.getClientId());
        body.add("client_version", String.valueOf(phonePeConfig.getClientVersion()));
        body.add("client_secret", phonePeConfig.getClientSecret());
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                phonePeConfig.getAuthUrl(),
                request,
                Map.class
        );

        Map responseBody = response.getBody();

        if (responseBody == null || responseBody.get("access_token") == null) {
            throw new RuntimeException("PhonePe access_token not received. Response: " + responseBody);
        }

        return responseBody.get("access_token").toString();
    }

    // ================= PHONEPE WEBHOOK / CALLBACK =================
    // POST /premium/callback
    @PostMapping("/premium/callback")
    public ResponseEntity<Object> callback(
            @RequestBody(required = false) Map<String, Object> requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            if (requestBody == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Webhook body is missing"));
            }

            String expectedAuth = sha256(
                    phonePeConfig.getWebhookUsername() + ":" + phonePeConfig.getWebhookPassword()
            );

            String receivedAuth = authorization == null ? "" : authorization.trim();

            if (receivedAuth.startsWith("SHA256 ")) {
                receivedAuth = receivedAuth.substring("SHA256 ".length()).trim();
            }

            if (!expectedAuth.equalsIgnoreCase(receivedAuth)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Invalid PhonePe webhook authorization"));
            }

            String event = String.valueOf(requestBody.get("event"));

            Object payloadObj = requestBody.get("payload");

            if (!(payloadObj instanceof Map)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "payload is missing"));
            }

            Map<String, Object> payload = (Map<String, Object>) payloadObj;

            String merchantOrderId = null;

            if (payload.get("merchantOrderId") != null) {
                merchantOrderId = payload.get("merchantOrderId").toString();
            } else if (payload.get("merchantTransactionId") != null) {
                merchantOrderId = payload.get("merchantTransactionId").toString();
            }

            if (merchantOrderId == null || merchantOrderId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "merchantOrderId is missing"));
            }

            String state = payload.get("state") == null ? "" : payload.get("state").toString();

            String transactionId = null;

            Object paymentDetailsObj = payload.get("paymentDetails");

            if (paymentDetailsObj instanceof List<?>) {
                List<?> paymentDetails = (List<?>) paymentDetailsObj;

                if (!paymentDetails.isEmpty() && paymentDetails.get(0) instanceof Map<?, ?>) {
                    Map<?, ?> firstPayment = (Map<?, ?>) paymentDetails.get(0);

                    Object txnIdObj = firstPayment.get("transactionId");

                    if (txnIdObj != null) {
                        transactionId = txnIdObj.toString();
                    }
                }
            }

            Optional<PaymentTransaction> txnOpt = paymentRepo.findByOrderId(merchantOrderId);

            if (txnOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Transaction not found but webhook received",
                        "merchantOrderId", merchantOrderId
                ));
            }

            PaymentTransaction txn = txnOpt.get();

            if (transactionId != null) {
                txn.setTransactionId(transactionId);
            }

            txn.setPaymentResponse(requestBody.toString());

            boolean paymentSuccess =
                    "checkout.order.completed".equalsIgnoreCase(event)
                            && "COMPLETED".equalsIgnoreCase(state);

            boolean paymentFailed =
                    "checkout.order.failed".equalsIgnoreCase(event)
                            || "FAILED".equalsIgnoreCase(state);

            if (paymentSuccess) {
                txn.setPaymentStatus(PaymentStatus.SUCCESS);
            } else if (paymentFailed) {
                txn.setPaymentStatus(PaymentStatus.FAILED);
            } else {
                txn.setPaymentStatus(PaymentStatus.PENDING);
            }

            paymentRepo.save(txn);

            // ================= USER PREMIUM WEBHOOK HANDLING =================
            if (txn.getUserId() != null) {

                Optional<User> userOpt = userRepository.findById(txn.getUserId());

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    if (txn.getPaymentStatus() == PaymentStatus.SUCCESS) {
                        user.setPaymentStatus("SUCCESS");
                        user.setPremiumStatus("PENDING");
                        user.setPremiumActive(false);

                        if (transactionId != null) {
                            user.setPhonePeTransactionId(transactionId);
                        }

                    } else if (txn.getPaymentStatus() == PaymentStatus.FAILED) {
                        user.setPaymentStatus("FAILED");
                        user.setPremiumStatus("REJECTED");
                        user.setPremiumActive(false);
                    }

                    userRepository.save(user);
                }

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "User premium webhook processed successfully"
                ));
            }

            // ================= PROPERTY PREMIUM WEBHOOK HANDLING =================
            if (txn.getPropertyId() != null) {

                Optional<Property> propOpt = propertyRepository.findById(txn.getPropertyId());

                if (propOpt.isPresent()) {
                    Property property = propOpt.get();

                    if (txn.getPaymentStatus() == PaymentStatus.SUCCESS) {
                        property.setPremiumStatus(PremiumStatus.PENDING_APPROVAL);
                        property.setPaymentStatus("SUCCESS");

                        if (transactionId != null) {
                            property.setPaymentTransactionId(transactionId);
                        }

                        property.setPaymentDate(LocalDateTime.now());

                    } else if (txn.getPaymentStatus() == PaymentStatus.FAILED) {
                        property.setPremiumStatus(PremiumStatus.REJECTED);
                        property.setPaymentStatus("FAILED");
                    }

                    propertyRepository.save(property);
                }

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Property premium webhook processed successfully"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed but no userId/propertyId found"
            ));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Webhook processing failed",
                            "error", e.getMessage()
                    ));
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("SHA-256 generation failed");
        }
    }

    // ================= PROPERTY PREMIUM STATUS =================
    // GET /premium/status/{propertyId}
    @GetMapping("/premium/status/{propertyId}")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER', 'ADMIN')")
    public ResponseEntity<Object> getPremiumStatus(@PathVariable Long propertyId) {

        Long loggedInOwnerId = getLoggedInOwnerId();

        if (loggedInOwnerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);

        if (property == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Property not found"));
        }

        if (!isAdmin()
                && (property.getPropertyOwner() == null
                || !property.getPropertyOwner().getOwnerId().equals(loggedInOwnerId))) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Forbidden: You do not own this property"));
        }

        long daysRemaining = 0;

        if (property.getPremiumEndDate() != null
                && (property.getPremiumStatus() == PremiumStatus.ACTIVE
                || property.getPremiumStatus() == PremiumStatus.FREE_ACTIVE)) {

            daysRemaining = java.time.Duration
                    .between(LocalDateTime.now(), property.getPremiumEndDate())
                    .toDays();

            if (daysRemaining < 0) {
                daysRemaining = 0;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("propertyName", property.getTitle());
        response.put("premiumStatus", getFriendlyStatus(property.getPremiumStatus()));
        response.put("premiumExpiryDate", property.getPremiumEndDate());
        response.put("paymentStatus", property.getPaymentStatus());
        response.put("daysRemaining", daysRemaining);

        return ResponseEntity.ok(response);
    }

    // ================= ADMIN PROPERTY PREMIUM PENDING =================
    // GET /admin/premium/pending
    @GetMapping("/admin/premium/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getPendingPremiumRequests() {

        List<Property> properties = propertyRepository.findByPremiumStatus(PremiumStatus.PENDING_APPROVAL);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Property property : properties) {

            Map<String, Object> map = new HashMap<>();
            map.put("propertyId", property.getId());
            map.put("title", property.getTitle());
            map.put("price", property.getPrice());
            map.put("city", property.getCity());
            map.put("location", property.getLocation());
            map.put("propertyType", property.getPropertyType());

            PropertyOwner owner = property.getPropertyOwner();

            if (owner != null) {
                map.put("ownerId", owner.getOwnerId());
                map.put("fullName", owner.getFullName());
                map.put("email", owner.getEmail());
                map.put("mobileNumber", owner.getMobileNumber());
            }

            map.put("paymentStatus", property.getPaymentStatus());
            map.put("paymentOrderId", property.getPaymentOrderId());
            map.put("paymentTransactionId", property.getPaymentTransactionId());
            map.put("paymentDate", property.getPaymentDate());

            response.add(map);
        }

        return ResponseEntity.ok(response);
    }

    // ================= ADMIN PROPERTY PREMIUM APPROVE =================
    // PUT /admin/premium/approve/{propertyId}
    @PutMapping("/admin/premium/approve/{propertyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> approvePremium(@PathVariable Long propertyId) {

        Property property = propertyRepository.findById(propertyId).orElse(null);

        if (property == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Property not found"));
        }

        if (property.getPremiumStatus() == PremiumStatus.ACTIVE
                || property.getPremiumStatus() == PremiumStatus.FREE_ACTIVE) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Cannot approve: Premium is already active"));
        }

        if (property.getPremiumStatus() == PremiumStatus.REJECTED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Cannot approve: Premium request was rejected"));
        }

        if (property.getPremiumStatus() != PremiumStatus.PENDING_APPROVAL) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Cannot approve: No pending payment or approval request found"));
        }

        property.setPremiumStatus(PremiumStatus.ACTIVE);
        property.setPremiumStartDate(LocalDateTime.now());
        property.setPremiumEndDate(LocalDateTime.now().plusDays(30));
        property.setPremiumApprovedBy(getLoggedInUsername());
        property.setPremiumApprovedDate(LocalDateTime.now());
        property.setPremiumActive(true);
        property.setPaymentStatus("APPROVED");
        property.setStatus(AppConstants.ACTIVE);

        propertyRepository.save(property);

        PropertyOwner owner = property.getPropertyOwner();

        if (owner != null) {
            owner.setPremiumActive(true);
            owner.setPremiumStatus("APPROVED");
            propertyOwnerRepository.save(owner);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Property premium approved successfully"
        ));
    }

    // ================= ADMIN PROPERTY PREMIUM REJECT =================
    // PUT /admin/premium/reject/{propertyId}
    @PutMapping("/admin/premium/reject/{propertyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> rejectPremium(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body
    ) {

        Property property = propertyRepository.findById(propertyId).orElse(null);

        if (property == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Property not found"));
        }

        if (property.getPremiumStatus() == PremiumStatus.ACTIVE
                || property.getPremiumStatus() == PremiumStatus.FREE_ACTIVE) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Cannot reject: Premium is already active"));
        }

        if (property.getPremiumStatus() != PremiumStatus.PENDING_APPROVAL) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Cannot reject: No pending premium request found"));
        }

        String rejectionReason = reason;

        if (rejectionReason == null && body != null) {
            rejectionReason = body.get("reason");
        }

        if (rejectionReason == null) {
            rejectionReason = "Rejected by admin";
        }

        property.setPremiumStatus(PremiumStatus.REJECTED);
        property.setRejectionReason(rejectionReason);
        property.setPremiumActive(false);
        property.setPaymentStatus("REJECTED");

        propertyRepository.save(property);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Property premium rejected successfully"
        ));
    }
}
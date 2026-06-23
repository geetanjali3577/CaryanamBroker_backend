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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class PremiumSubscriptionController {

    private static final int STALE_PENDING_MINUTES = 25;

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
                || property.getPremiumStatus() == PremiumStatus.FREE_ACTIVE
                || Boolean.TRUE.equals(property.getPremiumActive())) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Property already has an active premium subscription"));
        }

        Optional<PaymentTransaction> existingPending =
                paymentRepo.findFirstByPropertyIdAndPaymentStatus(propertyId, PaymentStatus.PENDING);

        if (existingPending.isPresent()) {
            PaymentTransaction oldTxn = existingPending.get();

            boolean stalePending =
                    oldTxn.getCreatedAt() != null
                            && oldTxn.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(STALE_PENDING_MINUTES));

            if (stalePending) {
                oldTxn.setPaymentStatus(PaymentStatus.CANCELLED);
                oldTxn.setPaymentResponse("Auto-cancelled because payment was pending for more than "
                        + STALE_PENDING_MINUTES + " minutes");
                paymentRepo.save(oldTxn);

                property.setPaymentStatus("CANCELLED");
                property.setPremiumStatus(PremiumStatus.REJECTED);
                property.setPremiumActive(false);
                propertyRepository.save(property);

                PropertyOwner owner = property.getPropertyOwner();
                if (owner != null) {
                    owner.setPaymentStatus("CANCELLED");
                    owner.setPremiumStatus("CANCELLED");
                    owner.setPremiumActive(false);
                    propertyOwnerRepository.save(owner);
                }

            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Payment already pending for this property"));
            }
        }

        // ================= TEST AMOUNT ₹1 =================
//        double baseAmount = 1.0;
//        double gstAmount = 0.0;
//        double totalAmount = 1.0;
//        long phonePeAmount = 100L;

        // ================= FINAL AMOUNT 99 + GST =================
         double baseAmount = 99.0;
         double gstAmount = 17.82;
         double totalAmount = 116.82;
         long phonePeAmount = Math.round(totalAmount * 100);

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

        String paymentUrl;

        try {
            paymentUrl = createPhonePePaymentUrl(orderId, phonePeAmount, propertyId);
        } catch (Exception e) {
            e.printStackTrace();

            markPropertyPaymentFailed(txn, property, "PhonePe payment URL creation failed: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "PhonePe payment URL creation failed",
                            "error", e.getMessage()
                    ));
        }

        // Important: PhonePe URL successfully create झाल्यावरच PENDING set करायचं
        property.setPremiumStatus(PremiumStatus.PAYMENT_PENDING);
        property.setPaymentOrderId(orderId);
        property.setPaymentStatus("PENDING");
        property.setPaymentAmount(totalAmount);
        property.setPremiumActive(false);
        propertyRepository.save(property);

        PropertyOwner owner = property.getPropertyOwner();
        if (owner != null) {
            owner.setPaymentStatus("PENDING");
            owner.setPremiumStatus("PAYMENT_PENDING");
            owner.setPremiumActive(false);
            owner.setPhonePeOrderId(orderId);
            owner.setPremiumAmount(totalAmount);
            owner.setPremiumCount((owner.getPremiumCount() == null ? 0 : owner.getPremiumCount()) + 1);
            propertyOwnerRepository.save(owner);
        }

        PremiumPaymentResponseDto response = new PremiumPaymentResponseDto(
                orderId,
                totalAmount,
                paymentUrl,
                "PENDING"
        );

        return ResponseEntity.ok(response);
    }

    // ================= PHONEPE V2 OAuth: Get Access Token =================
    private String getOAuthAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", phonePeConfig.getClientId());
            body.add("client_secret", phonePeConfig.getClientSecret());
            body.add("client_version", phonePeConfig.getClientVersion());
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            System.out.println("========== PhonePe V2 OAuth Token Request ==========");
            System.out.println("URL: " + phonePeConfig.getAuthUrl());
            System.out.println("ClientId: " + phonePeConfig.getClientId());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    phonePeConfig.getAuthUrl(),
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();
            System.out.println("OAuth Response: " + responseBody);

            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException("Failed to get PhonePe OAuth token. Response: " + responseBody);
            }

            return responseBody.get("access_token").toString();

        } catch (RestClientResponseException e) {
            System.out.println("PhonePe OAuth Error Status: " + e.getStatusCode().value());
            System.out.println("PhonePe OAuth Error Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("PhonePe OAuth token failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("PhonePe OAuth token failed: " + e.getMessage(), e);
        }
    }

    // ================= PHONEPE V2 CREATE PAYMENT URL =================
    private String createPhonePePaymentUrl(String orderId, Long amountInPaisa, Long propertyId) {
        try {
            String accessToken = getOAuthAccessToken();

            Map<String, Object> payload = new HashMap<>();
            payload.put("merchantOrderId", orderId);
            payload.put("amount", amountInPaisa);
            payload.put("expireAfter", 1200);

            Map<String, Object> metaInfo = new HashMap<>();
            metaInfo.put("udf1", "PROPERTY_" + propertyId);
            payload.put("metaInfo", metaInfo);

            Map<String, Object> merchantUrls = new HashMap<>();
            merchantUrls.put(
                    "redirectUrl",
                    appendQueryParams(phonePeConfig.getRedirectUrl(), orderId, "property")
            );

            Map<String, Object> paymentFlowConfig = new HashMap<>();
            paymentFlowConfig.put("type", "PG_CHECKOUT");
            paymentFlowConfig.put("message", "Property premium payment");
            paymentFlowConfig.put("merchantUrls", merchantUrls);

            payload.put("paymentFlow", paymentFlowConfig);

            String jsonPayload =
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);

            System.out.println("========== PhonePe V2 Pay Request (Property) ==========");
            System.out.println("URL: " + phonePeConfig.getPayUrl());
            System.out.println("OrderId: " + orderId);
            System.out.println("Amount (paise): " + amountInPaisa);
            System.out.println("Payload: " + jsonPayload);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // IMPORTANT: PhonePe V2 needs O-Bearer, not Bearer
            headers.set("Authorization", "O-Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    phonePeConfig.getPayUrl(),
                    request,
                    Map.class
            );

            System.out.println("========== PhonePe V2 Response ==========");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());

            Map respBody = response.getBody();

            if (respBody == null) {
                throw new RuntimeException("PhonePe V2 response body is null");
            }

            Object redirectUrlObj = respBody.get("redirectUrl");

            if (redirectUrlObj == null || redirectUrlObj.toString().isBlank()) {
                throw new RuntimeException("PhonePe V2 did not return redirectUrl. Response: " + respBody);
            }

            return redirectUrlObj.toString();

        } catch (RestClientResponseException e) {
            System.out.println("PhonePe Pay Error Status: " + e.getStatusCode().value());
            System.out.println("PhonePe Pay Error Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Error during PhonePe V2 order creation: " + e.getMessage()
                    + " | Body: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error during PhonePe V2 order creation: " + e.getMessage(), e);
        }
    }

    private String appendQueryParams(String redirectUrl, String orderId, String type) {
        String separator = redirectUrl.contains("?") ? "&" : "?";
        return redirectUrl + separator + "orderId=" + orderId + "&type=" + type;
    }

    private void markPropertyPaymentFailed(PaymentTransaction txn, Property property, String message) {
        txn.setPaymentStatus(PaymentStatus.FAILED);
        txn.setPaymentResponse(message);
        paymentRepo.save(txn);

        property.setPaymentStatus("FAILED");
        property.setPremiumStatus(PremiumStatus.REJECTED);
        property.setPremiumActive(false);
        propertyRepository.save(property);

        PropertyOwner owner = property.getPropertyOwner();
        if (owner != null) {
            owner.setPaymentStatus("FAILED");
            owner.setPremiumStatus("FAILED");
            owner.setPremiumActive(false);
            propertyOwnerRepository.save(owner);
        }
    }

    private void markLinkedEntitiesAfterVerification(
            PaymentTransaction txn,
            PaymentStatus paymentStatus,
            String transactionId
    ) {
        txn.setPaymentStatus(paymentStatus);

        if (transactionId != null && !transactionId.isBlank()) {
            txn.setTransactionId(transactionId);
        }

        paymentRepo.save(txn);

        if (txn.getUserId() != null) {
            User user = userRepository.findById(txn.getUserId()).orElse(null);

            if (user != null) {
                if (paymentStatus == PaymentStatus.SUCCESS) {
                    user.setPaymentStatus("SUCCESS");
                    user.setPremiumStatus("PENDING");
                    user.setPremiumActive(false);

                    if (transactionId != null && !transactionId.isBlank()) {
                        user.setPhonePeTransactionId(transactionId);
                    }

                } else if (paymentStatus == PaymentStatus.FAILED) {
                    user.setPaymentStatus("FAILED");
                    user.setPremiumStatus("FAILED");
                    user.setPremiumActive(false);

                } else if (paymentStatus == PaymentStatus.CANCELLED) {
                    user.setPaymentStatus("CANCELLED");
                    user.setPremiumStatus("CANCELLED");
                    user.setPremiumActive(false);

                } else {
                    user.setPaymentStatus("PENDING");
                    user.setPremiumStatus("PAYMENT_PENDING");
                    user.setPremiumActive(false);
                }

                userRepository.save(user);
            }

            return;
        }

        if (txn.getPropertyId() != null) {
            Property property = propertyRepository.findById(txn.getPropertyId()).orElse(null);

            if (property != null) {
                if (paymentStatus == PaymentStatus.SUCCESS) {
                    property.setPremiumStatus(PremiumStatus.PENDING_APPROVAL);
                    property.setPaymentStatus("SUCCESS");
                    property.setPremiumActive(false);

                    if (transactionId != null && !transactionId.isBlank()) {
                        property.setPaymentTransactionId(transactionId);
                    }

                    property.setPaymentDate(LocalDateTime.now());

                    PropertyOwner owner = property.getPropertyOwner();
                    if (owner != null) {
                        owner.setPaymentStatus("SUCCESS");
                        owner.setPremiumStatus("PENDING_APPROVAL");
                        owner.setPremiumActive(false);

                        if (transactionId != null && !transactionId.isBlank()) {
                            owner.setPhonePeTransactionId(transactionId);
                        }

                        propertyOwnerRepository.save(owner);
                    }

                } else if (paymentStatus == PaymentStatus.FAILED) {
                    property.setPremiumStatus(PremiumStatus.REJECTED);
                    property.setPaymentStatus("FAILED");
                    property.setPremiumActive(false);

                    PropertyOwner owner = property.getPropertyOwner();
                    if (owner != null) {
                        owner.setPaymentStatus("FAILED");
                        owner.setPremiumStatus("FAILED");
                        owner.setPremiumActive(false);
                        propertyOwnerRepository.save(owner);
                    }

                } else if (paymentStatus == PaymentStatus.CANCELLED) {
                    property.setPremiumStatus(PremiumStatus.REJECTED);
                    property.setPaymentStatus("CANCELLED");
                    property.setPremiumActive(false);

                    PropertyOwner owner = property.getPropertyOwner();
                    if (owner != null) {
                        owner.setPaymentStatus("CANCELLED");
                        owner.setPremiumStatus("CANCELLED");
                        owner.setPremiumActive(false);
                        propertyOwnerRepository.save(owner);
                    }

                } else {
                    property.setPremiumStatus(PremiumStatus.PAYMENT_PENDING);
                    property.setPaymentStatus("PENDING");
                    property.setPremiumActive(false);
                }

                propertyRepository.save(property);
            }
        }
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
            String transactionId = extractTransactionId(payload.get("paymentDetails"));

            Optional<PaymentTransaction> txnOpt = paymentRepo.findByOrderId(merchantOrderId);

            if (txnOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Transaction not found but webhook received",
                        "merchantOrderId", merchantOrderId
                ));
            }

            PaymentTransaction txn = txnOpt.get();
            txn.setPaymentResponse(requestBody.toString());

            boolean paymentSuccess =
                    "checkout.order.completed".equalsIgnoreCase(event)
                            && "COMPLETED".equalsIgnoreCase(state);

            boolean paymentFailed =
                    "checkout.order.failed".equalsIgnoreCase(event)
                            || "FAILED".equalsIgnoreCase(state);

            boolean paymentCancelled =
                    "checkout.order.cancelled".equalsIgnoreCase(event)
                            || "CANCELLED".equalsIgnoreCase(state)
                            || "EXPIRED".equalsIgnoreCase(state);

            if (paymentSuccess) {
                markLinkedEntitiesAfterVerification(txn, PaymentStatus.SUCCESS, transactionId);
            } else if (paymentFailed) {
                markLinkedEntitiesAfterVerification(txn, PaymentStatus.FAILED, transactionId);
            } else if (paymentCancelled) {
                markLinkedEntitiesAfterVerification(txn, PaymentStatus.CANCELLED, transactionId);
            } else {
                markLinkedEntitiesAfterVerification(txn, PaymentStatus.PENDING, transactionId);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed successfully"
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

    private String extractTransactionId(Object paymentDetailsObj) {
        if (paymentDetailsObj instanceof List<?>) {
            List<?> paymentDetails = (List<?>) paymentDetailsObj;

            if (!paymentDetails.isEmpty() && paymentDetails.get(0) instanceof Map<?, ?>) {
                Map<?, ?> firstPayment = (Map<?, ?>) paymentDetails.get(0);

                Object txnIdObj = firstPayment.get("transactionId");

                if (txnIdObj != null) {
                    return txnIdObj.toString();
                }
            }
        }

        return null;
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
        response.put("premiumStatus", property.getPremiumStatus() != null ? property.getPremiumStatus().name() : "NONE");
        response.put("friendlyStatus", getFriendlyStatus(property.getPremiumStatus()));
        response.put("premiumExpiryDate", property.getPremiumEndDate());
        response.put("paymentStatus", property.getPaymentStatus());
        response.put("paymentOrderId", property.getPaymentOrderId());
        response.put("paymentTransactionId", property.getPaymentTransactionId());
        response.put("paymentDate", property.getPaymentDate());
        response.put("premiumActive", property.getPremiumActive());
        response.put("daysRemaining", daysRemaining);

        return ResponseEntity.ok(response);
    }

    // ================= PHONEPE V2 STATUS VERIFICATION =================
    // GET /premium/verify/{orderId}
    @GetMapping("/premium/verify/{orderId}")
    public ResponseEntity<Object> verifyPayment(@PathVariable String orderId) {
        try {
            Optional<PaymentTransaction> txnOpt = paymentRepo.findByOrderId(orderId);

            if (txnOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Transaction not found"));
            }

            PaymentTransaction txn = txnOpt.get();

            String accessToken = getOAuthAccessToken();

            String statusUrl =
                    "https://api.phonepe.com/apis/pg/checkout/v2/order/" + orderId + "/status";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "O-Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            System.out.println("========== PhonePe V2 Status Verify ==========");
            System.out.println("URL: " + statusUrl);

            ResponseEntity<Map> response = restTemplate.exchange(
                    statusUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            System.out.println("========== PhonePe V2 Status Response ==========");
            System.out.println("Response: " + response.getBody());

            Map body = response.getBody();

            if (body == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Empty response from PhonePe"));
            }

            txn.setPaymentResponse(body.toString());

            String state = body.get("state") == null ? "" : body.get("state").toString();
            String transactionId = extractTransactionId(body.get("paymentDetails"));

            Long expireAt = null;
            if (body.get("expireAt") instanceof Number) {
                expireAt = ((Number) body.get("expireAt")).longValue();
            }

            boolean expired = expireAt != null && System.currentTimeMillis() > expireAt;

            if ("COMPLETED".equalsIgnoreCase(state)) {
                markLinkedEntitiesAfterVerification(txn, PaymentStatus.SUCCESS, transactionId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "status", "SUCCESS",
                        "message", "Payment verified as successful"
                ));

            } else if ("FAILED".equalsIgnoreCase(state)) {
                markLinkedEntitiesAfterVerification(txn, PaymentStatus.FAILED, transactionId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "status", "FAILED",
                        "message", "Payment verified as failed"
                ));

            } else if ("CANCELLED".equalsIgnoreCase(state)
                    || "EXPIRED".equalsIgnoreCase(state)
                    || expired) {

                markLinkedEntitiesAfterVerification(txn, PaymentStatus.CANCELLED, transactionId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "status", "CANCELLED",
                        "message", "Payment cancelled or expired"
                ));

            } else {
                markLinkedEntitiesAfterVerification(txn, PaymentStatus.PENDING, transactionId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "status", "PENDING",
                        "message", "Payment is still pending"
                ));
            }

        } catch (RestClientResponseException e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Status verification failed",
                            "error", e.getMessage(),
                            "body", e.getResponseBodyAsString()
                    ));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Status verification failed",
                            "error", e.getMessage()
                    ));
        }
    }

    // ================= ADMIN PROPERTY PREMIUM PENDING =================
    // GET /admin/premium/pending
    @GetMapping("/admin/premium/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getPendingPremiumRequests() {

        List<Property> properties = propertyRepository.findByPremiumStatus(PremiumStatus.PENDING_APPROVAL);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Property property : properties) {
            if (!"SUCCESS".equalsIgnoreCase(property.getPaymentStatus())) {
                continue;
            }

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
            owner.setPaymentStatus("APPROVED");
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

        PropertyOwner owner = property.getPropertyOwner();
        if (owner != null) {
            owner.setPremiumActive(false);
            owner.setPremiumStatus("REJECTED");
            owner.setPaymentStatus("REJECTED");
            propertyOwnerRepository.save(owner);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Property premium rejected successfully"
        ));
    }

    @GetMapping("/premium/check-status/{orderId}")
    public ResponseEntity<?> checkStatus(@PathVariable String orderId) {
        Optional<PaymentTransaction> txnOpt = paymentRepo.findByOrderId(orderId);

        if (txnOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Order not found"));
        }

        PaymentTransaction txn = txnOpt.get();

        return ResponseEntity.ok(
                Map.of(
                        "orderId", txn.getOrderId(),
                        "status", txn.getPaymentStatus(),
                        "transactionId", txn.getTransactionId()
                )
        );
    }
}
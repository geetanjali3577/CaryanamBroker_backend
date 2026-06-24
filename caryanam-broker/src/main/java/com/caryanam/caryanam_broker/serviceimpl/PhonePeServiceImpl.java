//package com.caryanam.caryanam_broker.serviceimpl;
//
//import com.caryanam.caryanam_broker.configuration.PhonePeConfig;
//import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderRequestDto;
//import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderResponseDto;
//import com.caryanam.caryanam_broker.dto.PremiumPaymentResponseDto;
//import com.caryanam.caryanam_broker.Enum.PaymentStatus;
//import com.caryanam.caryanam_broker.entity.PaymentTransaction;
//import com.caryanam.caryanam_broker.entity.Property;
//import com.caryanam.caryanam_broker.entity.PropertyOwner;
//import com.caryanam.caryanam_broker.entity.User;
//import com.caryanam.caryanam_broker.enums.PremiumStatus;
//import com.caryanam.caryanam_broker.repository.PaymentTransactionRepository;
//import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
//import com.caryanam.caryanam_broker.repository.PropertyRepository;
//import com.caryanam.caryanam_broker.repository.UserRepository;
//import com.caryanam.caryanam_broker.service.PhonePeService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//public class PhonePeServiceImpl implements PhonePeService {
//    @Autowired
//    private PhonePeConfig phonePeConfig;
//    @Autowired
//    private final UserRepository userRepository;
//    @Autowired
//    private PaymentTransactionRepository paymentRepo;
//    @Autowired
//    private PropertyRepository propertyRepository;
//    @Autowired
//    private PropertyOwnerRepository propertyOwnerRepository;
//
//    // ================= V2 OAuth: Get Access Token =================
//    @Override
//    public String getAccessToken() {
//        try {
//            RestTemplate restTemplate = new RestTemplate();
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//            body.add("client_id", phonePeConfig.getClientId());
//            body.add("client_secret", phonePeConfig.getClientSecret());
//            body.add("client_version", phonePeConfig.getClientVersion());
//            body.add("grant_type", "client_credentials");
//
//            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
//
//            System.out.println("========== PhonePe V2 OAuth Token Request ==========");
//            System.out.println("URL: " + phonePeConfig.getAuthUrl());
//            System.out.println("ClientId: " + phonePeConfig.getClientId());
//
//            System.out.println("AUTH URL = " + phonePeConfig.getAuthUrl());
//            System.out.println("CLIENT ID = " + phonePeConfig.getClientId());
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(
//                    phonePeConfig.getAuthUrl(),
//                    request,
//                    Map.class
//            );
//
//            Map responseBody = response.getBody();
//            System.out.println("OAuth Response: " + responseBody);
//
//            if (responseBody == null || !responseBody.containsKey("access_token")) {
//                throw new RuntimeException("Failed to get PhonePe OAuth token. Response: " + responseBody);
//            }
//
//            return responseBody.get("access_token").toString();
//
//        } catch (Exception e) {
//            System.err.println("PhonePe OAuth token error: " + e.getMessage());
//            e.printStackTrace();
//            throw new RuntimeException("Failed to get PhonePe OAuth access token: " + e.getMessage(), e);
//        }
//    }
//
//    // ================= V2 PG_CHECKOUT: Create Premium Order (Web) =================
//    @Override
//    public PremiumPaymentResponseDto createPremiumOrder(Long userId) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//
//        String orderId = "PREMIUM_" + System.currentTimeMillis();
//
//        // TEST AMOUNT ₹1
////        double baseAmount = 1.0;
////        double gstAmount = 0.0;
////        double totalAmount = 1.0;
////        long phonePeAmount = 100L; // ₹1 = 100 paisa
//        double baseAmount = 99.0;
//        double gstAmount = 17.82;
//        double totalAmount = 116.82;
//        long phonePeAmount = Math.round(totalAmount * 100);
//
//        // Set user state to PAYMENT_PENDING before calling PhonePe
//        user.setPhonePeOrderId(orderId);
//        user.setPaymentStatus("PENDING");
//        user.setPremiumStatus("PAYMENT_PENDING");
//        user.setPremiumAmount(totalAmount);
//        user.setPremiumActive(false);
//        user.setPremiumCount(user.getPremiumCount() + 1);
//        userRepository.save(user);
//
//        // Create PaymentTransaction as PENDING
//        PaymentTransaction txn = new PaymentTransaction();
//        txn.setUserId(userId);
//        txn.setOrderId(orderId);
//        txn.setAmount(totalAmount);
//        txn.setBaseAmount(baseAmount);
//        txn.setGstAmount(gstAmount);
//        txn.setTotalAmount(totalAmount);
//        txn.setPaymentGateway("PhonePe");
//        txn.setPaymentStatus(PaymentStatus.PENDING);
//        txn.setCreatedAt(LocalDateTime.now());
//        paymentRepo.save(txn);
//
//        try {
//            // Step 1: Get OAuth access token
//            String accessToken = getAccessToken();
//
//            // Step 2: Build V2 PG_CHECKOUT payload
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("merchantOrderId", orderId);
//            payload.put("amount", phonePeAmount);
//            payload.put("expireAfter", 1200); // 20 minutes
//
//            Map<String, Object> paymentFlow = new HashMap<>();
//            Map<String, Object> payPageConfig = new HashMap<>();
//            payPageConfig.put("redirectUrl", phonePeConfig.getRedirectUrl());
//            paymentFlow.put("paymentFlow", Map.of("type", "PG_CHECKOUT", "merchantUrls", payPageConfig));
//
//            // Merge paymentFlow into payload
//            payload.put("paymentFlow", paymentFlow.get("paymentFlow"));
//
//            String jsonPayload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
//
//            System.out.println("========== PhonePe V2 Pay Request ==========");
//            System.out.println("URL: " + phonePeConfig.getPayUrl());
//            System.out.println("OrderId: " + orderId);
//            System.out.println("Amount (paise): " + phonePeAmount);
//            System.out.println("Payload: " + jsonPayload);
//
//            // Step 3: Call PhonePe V2 checkout API
//            RestTemplate restTemplate = new RestTemplate();
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("Authorization", "O-Bearer " + accessToken);
//
//            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(
//                    phonePeConfig.getPayUrl(),
//                    request,
//                    Map.class
//            );
//
//            System.out.println("========== PhonePe V2 Response ==========");
//            System.out.println("Status: " + response.getStatusCode());
//            System.out.println("Body: " + response.getBody());
//
//            Map body = response.getBody();
//            if (body == null) {
//                throw new RuntimeException("PhonePe V2 response body is null");
//            }
//
//            // V2 response: { "orderId": "...", "state": "PENDING", "redirectUrl": "https://..." }
//            String redirectUrl = null;
//            if (body.containsKey("redirectUrl")) {
//                redirectUrl = body.get("redirectUrl").toString();
//            }
//
//            if (redirectUrl == null || redirectUrl.isBlank()) {
//                throw new RuntimeException("PhonePe V2 did not return a redirectUrl. Response: " + body);
//            }
//
//            // Save PhonePe's orderId if available
//            if (body.containsKey("orderId")) {
//                txn.setTransactionId(body.get("orderId").toString());
//                paymentRepo.save(txn);
//            }
//
//            return new PremiumPaymentResponseDto(
//                    orderId,
//                    totalAmount,
//                    redirectUrl,
//                    "PENDING"
//            );
//
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            // Mark payment as FAILED on any error
//            txn.setPaymentStatus(PaymentStatus.FAILED);
//            txn.setPaymentResponse("PhonePe V2 payment initiation failed: " + e.getMessage());
//            paymentRepo.save(txn);
//
//            user.setPaymentStatus("FAILED");
//            user.setPremiumStatus("REJECTED");
//            userRepository.save(user);
//
//            throw new RuntimeException("Error during PhonePe V2 order creation: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public void paymentSuccess(
//            String orderId,
//            String transactionId) {
//
//        PaymentTransaction txn =
//                paymentRepo.findByOrderId(orderId)
//                        .orElseThrow();
//
//        txn.setPaymentStatus(PaymentStatus.SUCCESS);
//        txn.setTransactionId(transactionId);
//        paymentRepo.save(txn);
//
//        User user =
//                userRepository.findById(txn.getUserId())
//                        .orElseThrow();
//        user.setPaymentStatus("SUCCESS");
//        user.setPhonePeTransactionId(transactionId);
//
//        userRepository.save(user);
//    }
//
//    @Override
//    public void propertyPaymentSuccess(String orderId, String transactionId) {
//        // Property payment handled by PremiumSubscriptionController webhook
//    }
//
//    // ================= MOBILE SDK: Create Order Token =================
//    @Override
//    public PhonePeMobileCreateOrderResponseDto createMobileOrder(PhonePeMobileCreateOrderRequestDto requestDto) {
//        try {
//            // Generate unique merchantOrderId for mobile
//            String merchantOrderId = "RC_MOBILE_" + System.currentTimeMillis();
//
//            // Amount is expected in rupees from APK, convert to paisa
//            long amountInPaisa = requestDto.getAmount() * 100;
//
//            // Step 1: Get OAuth access token
//            String accessToken = getAccessToken();
//
//            // Step 2: Build Mobile SDK Create Order Token payload
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("merchantOrderId", merchantOrderId);
//            payload.put("amount", amountInPaisa);
//            payload.put("expireAfter", 1200); // 20 minutes
//
//            // MetaInfo to carry context
//            Map<String, Object> metaInfo = new HashMap<>();
//            metaInfo.put("udf1", requestDto.getPremiumFor()); // USER or OWNER
//            metaInfo.put("udf2", String.valueOf(requestDto.getUserId()));
//            metaInfo.put("udf3", String.valueOf(requestDto.getOwnerId()));
//            metaInfo.put("udf4", String.valueOf(requestDto.getPropertyId()));
//            payload.put("metaInfo", metaInfo);
//
//            // Payment flow for mobile SDK - UPI_INTENT only, NO redirectUrl
//            Map<String, Object> paymentModeConfig = new HashMap<>();
//            List<Map<String, String>> enabledPaymentModes = new ArrayList<>();
//            Map<String, String> upiIntent = new HashMap<>();
//            upiIntent.put("type", "UPI_INTENT");
//            enabledPaymentModes.add(upiIntent);
//            paymentModeConfig.put("enabledPaymentModes", enabledPaymentModes);
//
//            Map<String, Object> paymentFlow = new HashMap<>();
//            paymentFlow.put("type", "PG_CHECKOUT");
//            paymentFlow.put("paymentModeConfig", paymentModeConfig);
//            payload.put("paymentFlow", paymentFlow);
//
//            String jsonPayload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
//
//            System.out.println("========== PhonePe Mobile SDK Create Order Request ==========");
//            System.out.println("URL: " + phonePeConfig.getSdkOrderUrl());
//            System.out.println("MerchantOrderId: " + merchantOrderId);
//            System.out.println("Amount (paise): " + amountInPaisa);
//            System.out.println("Payload: " + jsonPayload);
//
//            // Step 3: Call PhonePe Mobile SDK Create Order Token API
//            RestTemplate restTemplate = new RestTemplate();
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("Authorization", "O-Bearer " + accessToken);
//
//            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(
//                    phonePeConfig.getSdkOrderUrl(),
//                    request,
//                    Map.class
//            );
//
//            System.out.println("========== PhonePe Mobile SDK Response ==========");
//            System.out.println("Status: " + response.getStatusCode());
//            System.out.println("Body: " + response.getBody());
//
//            Map body = response.getBody();
//            if (body == null) {
//                throw new RuntimeException("PhonePe Mobile SDK response body is null");
//            }
//
//            // Extract orderId and token from response
//            String phonePeOrderId = body.get("orderId") != null ? body.get("orderId").toString() : null;
//            String token = body.get("token") != null ? body.get("token").toString() : null;
//
//            if (token == null || token.isBlank()) {
//                throw new RuntimeException("PhonePe Mobile SDK did not return a token. Response: " + body);
//            }
//
//            // Step 4: Save transaction as PENDING (do NOT update property/user premium status yet)
//            PaymentTransaction txn = new PaymentTransaction();
//            txn.setMerchantOrderId(merchantOrderId);
//            txn.setOrderId(merchantOrderId); // also set orderId for backward compatibility
//            txn.setTransactionId(phonePeOrderId);
//            txn.setPaymentStatus(PaymentStatus.PENDING);
//            txn.setPremiumFor(requestDto.getPremiumFor());
//            txn.setUserId(requestDto.getUserId());
//            txn.setOwnerId(requestDto.getOwnerId());
//            txn.setPropertyId(requestDto.getPropertyId());
//            txn.setAmount(requestDto.getAmount().doubleValue());
//            txn.setTotalAmount(requestDto.getAmount().doubleValue());
//            txn.setPaymentGateway("PhonePe_Mobile_SDK");
//            txn.setCreatedAt(LocalDateTime.now());
//            paymentRepo.save(txn);
//
//            // Step 5: Return response to APK (NO redirectUrl, NO QR)
//            PhonePeMobileCreateOrderResponseDto responseDto = new PhonePeMobileCreateOrderResponseDto();
//            responseDto.setMerchantOrderId(merchantOrderId);
//            responseDto.setOrderId(phonePeOrderId);
//            responseDto.setToken(token);
//            responseDto.setMerchantId(phonePeConfig.getMerchantId());
//            responseDto.setEnvironment(phonePeConfig.getEnvironment());
//
//            return responseDto;
//
//        } catch (Exception e) {
//            System.err.println("PhonePe Mobile SDK create order error: " + e.getMessage());
//            e.printStackTrace();
//            throw new RuntimeException("Error during PhonePe Mobile SDK order creation: " + e.getMessage(), e);
//        }
//    }
//
//    // ================= MOBILE SDK: Verify Payment =================
//    @Override
//    public Object verifyMobilePayment(String merchantOrderId) {
//        try {
//            // Step 1: Find transaction by merchantOrderId
//            PaymentTransaction txn = paymentRepo.findByMerchantOrderId(merchantOrderId)
//                    .orElseThrow(() -> new RuntimeException("Transaction not found for merchantOrderId: " + merchantOrderId));
//
//            // Step 2: Get OAuth access token
//            String accessToken = getAccessToken();
//
//            // Step 3: Call PhonePe Order Status API
//            String statusUrl = phonePeConfig.getStatusUrl()
//                    .replace("{merchantOrderId}", merchantOrderId);
//
//            RestTemplate restTemplate = new RestTemplate();
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization", "O-Bearer " + accessToken);
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<String> entity = new HttpEntity<>(headers);
//
//            System.out.println("========== PhonePe Mobile SDK Verify Payment ==========");
//            System.out.println("URL: " + statusUrl);
//            System.out.println("MerchantOrderId: " + merchantOrderId);
//
//            ResponseEntity<Map> response = restTemplate.exchange(
//                    statusUrl,
//                    HttpMethod.GET,
//                    entity,
//                    Map.class
//            );
//
//            System.out.println("========== PhonePe Mobile SDK Status Response ==========");
//            System.out.println("Response: " + response.getBody());
//
//            Map body = response.getBody();
//            if (body == null) {
//                return Map.of("success", false, "message", "Empty response from PhonePe");
//            }
//
//            txn.setPaymentResponse(body.toString());
//
//            String state = body.get("state") == null ? "" : body.get("state").toString();
//
//            // Extract transactionId from paymentDetails if available
//            String transactionId = null;
//            if (body.get("paymentDetails") instanceof List<?>) {
//                List<?> paymentDetails = (List<?>) body.get("paymentDetails");
//                if (!paymentDetails.isEmpty() && paymentDetails.get(0) instanceof Map<?, ?>) {
//                    Map<?, ?> firstPayment = (Map<?, ?>) paymentDetails.get(0);
//                    Object txnIdObj = firstPayment.get("transactionId");
//                    if (txnIdObj != null) {
//                        transactionId = txnIdObj.toString();
//                    }
//                }
//            }
//
//            // Step 4: Process based on PhonePe state
//            if ("COMPLETED".equalsIgnoreCase(state)) {
//                // Payment SUCCESS
//                txn.setPaymentStatus(PaymentStatus.SUCCESS);
//                if (transactionId != null && !transactionId.isBlank()) {
//                    txn.setTransactionId(transactionId);
//                }
//                paymentRepo.save(txn);
//
//                // Update property/user premium status based on premiumFor
//                if ("OWNER".equalsIgnoreCase(txn.getPremiumFor())) {
//                    // OWNER/Property premium flow
//                    if (txn.getPropertyId() != null) {
//                        Property property = propertyRepository.findById(txn.getPropertyId()).orElse(null);
//                        if (property != null) {
//                            property.setPaymentStatus("SUCCESS");
//                            property.setPremiumStatus(PremiumStatus.PENDING_APPROVAL);
//                            property.setPremiumActive(false);
//                            property.setPaymentDate(LocalDateTime.now());
//                            if (transactionId != null && !transactionId.isBlank()) {
//                                property.setPaymentTransactionId(transactionId);
//                            }
//                            property.setPaymentOrderId(merchantOrderId);
//                            propertyRepository.save(property);
//                        }
//                    }
//
//                    if (txn.getOwnerId() != null) {
//                        PropertyOwner owner = propertyOwnerRepository.findById(txn.getOwnerId()).orElse(null);
//                        if (owner != null) {
//                            owner.setPaymentStatus("SUCCESS");
//                            owner.setPremiumStatus("PENDING_APPROVAL");
//                            owner.setPremiumActive(false);
//                            if (transactionId != null && !transactionId.isBlank()) {
//                                owner.setPhonePeTransactionId(transactionId);
//                            }
//                            propertyOwnerRepository.save(owner);
//                        }
//                    }
//
//                } else if ("USER".equalsIgnoreCase(txn.getPremiumFor())) {
//                    // USER premium flow
//                    if (txn.getUserId() != null) {
//                        User user = userRepository.findById(txn.getUserId()).orElse(null);
//                        if (user != null) {
//                            user.setPaymentStatus("SUCCESS");
//                            user.setPremiumStatus("PENDING");
//                            user.setPremiumActive(false);
//                            if (transactionId != null && !transactionId.isBlank()) {
//                                user.setPhonePeTransactionId(transactionId);
//                            }
//                            userRepository.save(user);
//                        }
//                    }
//                }
//
//                return Map.of(
//                        "success", true,
//                        "status", "SUCCESS",
//                        "message", "Payment verified as successful. Premium request sent for admin approval.",
//                        "merchantOrderId", merchantOrderId
//                );
//
//            } else if ("FAILED".equalsIgnoreCase(state)) {
//                // Payment FAILED
//                txn.setPaymentStatus(PaymentStatus.FAILED);
//                if (transactionId != null && !transactionId.isBlank()) {
//                    txn.setTransactionId(transactionId);
//                }
//                paymentRepo.save(txn);
//
//                // Do NOT update property/user premium status, do NOT send to admin pending
//                return Map.of(
//                        "success", true,
//                        "status", "FAILED",
//                        "message", "Payment verification failed.",
//                        "merchantOrderId", merchantOrderId
//                );
//
//            } else {
//                // PENDING or any other state
//                txn.setPaymentStatus(PaymentStatus.PENDING);
//                paymentRepo.save(txn);
//
//                // Do NOT update property/user, keep as PENDING
//                return Map.of(
//                        "success", true,
//                        "status", "PENDING",
//                        "message", "Payment is still pending.",
//                        "merchantOrderId", merchantOrderId
//                );
//            }
//
//        } catch (Exception e) {
//            System.err.println("PhonePe Mobile SDK verify payment error: " + e.getMessage());
//            e.printStackTrace();
//            return Map.of(
//                    "success", false,
//                    "status", "ERROR",
//                    "message", "Payment verification failed: " + e.getMessage(),
//                    "merchantOrderId", merchantOrderId
//            );
//        }
//    }
//}
package com.caryanam.caryanam_broker.serviceimpl;

import com.caryanam.caryanam_broker.configuration.PhonePeConfig;
import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderRequestDto;
import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderResponseDto;
import com.caryanam.caryanam_broker.dto.PremiumPaymentResponseDto;
import com.caryanam.caryanam_broker.Enum.PaymentStatus;
import com.caryanam.caryanam_broker.entity.PaymentTransaction;
import com.caryanam.caryanam_broker.entity.Property;
import com.caryanam.caryanam_broker.entity.PropertyOwner;
import com.caryanam.caryanam_broker.entity.User;
import com.caryanam.caryanam_broker.enums.PremiumStatus;
import com.caryanam.caryanam_broker.repository.PaymentTransactionRepository;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.PropertyRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import com.caryanam.caryanam_broker.service.PhonePeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PhonePeServiceImpl implements PhonePeService {

    private final PhonePeConfig phonePeConfig;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentRepo;
    private final PropertyRepository propertyRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;

    /*
      Production fixed premium amount.
      Frontend/APK amount वर trust करू नये.
    */
    private static final double PREMIUM_BASE_AMOUNT = 99.0;
    private static final double PREMIUM_GST_AMOUNT = 18.0;
    private static final double PREMIUM_TOTAL_AMOUNT = 117.0;
    private static final long PREMIUM_AMOUNT_IN_PAISA = 11700L;

    // ================= V2 OAuth: Get Access Token =================

    @Override
    public String getAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", phonePeConfig.getClientId());
            body.add("client_secret", phonePeConfig.getClientSecret());
            body.add("client_version", phonePeConfig.getClientVersion());
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            System.out.println("========== PhonePe V2 OAuth Token Request ==========");
            System.out.println("URL: " + phonePeConfig.getAuthUrl());
            System.out.println("CLIENT ID: " + phonePeConfig.getClientId());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    phonePeConfig.getAuthUrl(),
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();

            System.out.println("========== PhonePe OAuth Response ==========");
            System.out.println(responseBody);

            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException(
                        "Failed to get PhonePe OAuth token. Response: " + responseBody
                );
            }

            return responseBody.get("access_token").toString();

        } catch (Exception e) {
            System.err.println("PhonePe OAuth token error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(
                    "Failed to get PhonePe OAuth access token: " + e.getMessage(),
                    e
            );
        }
    }

    // ================= V2 PG_CHECKOUT: Create Premium Order WEB =================

    @Override
    public PremiumPaymentResponseDto createPremiumOrder(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id: " + userId)
                );

        String orderId = "PREMIUM_" + System.currentTimeMillis();

        user.setPhonePeOrderId(orderId);
        user.setPaymentStatus("PENDING");
        user.setPremiumStatus("PAYMENT_PENDING");
        user.setPremiumAmount(PREMIUM_TOTAL_AMOUNT);
        user.setPremiumActive(false);
        user.setPremiumCount(user.getPremiumCount() + 1);
        userRepository.save(user);

        PaymentTransaction txn = new PaymentTransaction();
        txn.setUserId(userId);
        txn.setOrderId(orderId);
        txn.setMerchantOrderId(orderId);
        txn.setAmount(PREMIUM_TOTAL_AMOUNT);
        txn.setBaseAmount(PREMIUM_BASE_AMOUNT);
        txn.setGstAmount(PREMIUM_GST_AMOUNT);
        txn.setTotalAmount(PREMIUM_TOTAL_AMOUNT);
        txn.setPaymentGateway("PhonePe_Web");
        txn.setPaymentStatus(PaymentStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(txn);

        try {
            String accessToken = getAccessToken();

            Map<String, Object> payload = new HashMap<>();
            payload.put("merchantOrderId", orderId);
            payload.put("amount", PREMIUM_AMOUNT_IN_PAISA);
            payload.put("expireAfter", 1200);

            Map<String, Object> merchantUrls = new HashMap<>();
            merchantUrls.put("redirectUrl", phonePeConfig.getRedirectUrl());

            Map<String, Object> paymentFlow = new HashMap<>();
            paymentFlow.put("type", "PG_CHECKOUT");
            paymentFlow.put("merchantUrls", merchantUrls);

            payload.put("paymentFlow", paymentFlow);

            String jsonPayload =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(payload);

            System.out.println("========== PhonePe V2 Web Pay Request ==========");
            System.out.println("URL: " + phonePeConfig.getPayUrl());
            System.out.println("MerchantOrderId: " + orderId);
            System.out.println("Amount Paise: " + PREMIUM_AMOUNT_IN_PAISA);
            System.out.println("Payload: " + jsonPayload);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "O-Bearer " + accessToken);

            HttpEntity<String> request =
                    new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    phonePeConfig.getPayUrl(),
                    request,
                    Map.class
            );

            System.out.println("========== PhonePe V2 Web Response ==========");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());

            Map body = response.getBody();

            if (body == null) {
                throw new RuntimeException("PhonePe V2 response body is null");
            }

            String redirectUrl =
                    body.get("redirectUrl") != null
                            ? body.get("redirectUrl").toString()
                            : null;

            if (redirectUrl == null || redirectUrl.isBlank()) {
                throw new RuntimeException(
                        "PhonePe V2 did not return redirectUrl. Response: " + body
                );
            }

            if (body.get("orderId") != null) {
                txn.setTransactionId(body.get("orderId").toString());
                paymentRepo.save(txn);
            }

            return new PremiumPaymentResponseDto(
                    orderId,
                    PREMIUM_TOTAL_AMOUNT,
                    redirectUrl,
                    "PENDING"
            );

        } catch (Exception e) {
            e.printStackTrace();

            txn.setPaymentStatus(PaymentStatus.FAILED);
            txn.setPaymentResponse(
                    "PhonePe web payment initiation failed: " + e.getMessage()
            );
            paymentRepo.save(txn);

            user.setPaymentStatus("FAILED");
            user.setPremiumStatus("REJECTED");
            user.setPremiumActive(false);
            userRepository.save(user);

            throw new RuntimeException(
                    "Error during PhonePe web order creation: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void paymentSuccess(String orderId, String transactionId) {

        PaymentTransaction txn =
                paymentRepo.findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException("Transaction not found: " + orderId)
                        );

        txn.setPaymentStatus(PaymentStatus.SUCCESS);
        txn.setTransactionId(transactionId);
        paymentRepo.save(txn);

        User user =
                userRepository.findById(txn.getUserId())
                        .orElseThrow(() ->
                                new RuntimeException("User not found")
                        );

        user.setPaymentStatus("SUCCESS");
        user.setPhonePeTransactionId(transactionId);
        userRepository.save(user);
    }

    @Override
    public void propertyPaymentSuccess(String orderId, String transactionId) {
        // Property payment handled by mobile verify / existing webhook flow.
    }

    // ================= MOBILE SDK: Create Order Token =================

    @Override
    public PhonePeMobileCreateOrderResponseDto createMobileOrder(
            PhonePeMobileCreateOrderRequestDto requestDto
    ) {
        try {
            String premiumFor = normalizePremiumFor(requestDto.getPremiumFor());

            validateMobileCreateOrderRequest(requestDto, premiumFor);

            String merchantOrderId =
                    "RC_MOBILE_" + System.currentTimeMillis();

            String accessToken = getAccessToken();

            Map<String, Object> payload = new HashMap<>();
            payload.put("merchantOrderId", merchantOrderId);
            payload.put("amount", PREMIUM_AMOUNT_IN_PAISA);
            payload.put("expireAfter", 1200);

            Map<String, Object> metaInfo = new HashMap<>();
            metaInfo.put("udf1", premiumFor);
            metaInfo.put(
                    "udf2",
                    requestDto.getUserId() != null
                            ? String.valueOf(requestDto.getUserId())
                            : ""
            );
            metaInfo.put(
                    "udf3",
                    requestDto.getOwnerId() != null
                            ? String.valueOf(requestDto.getOwnerId())
                            : ""
            );
            metaInfo.put(
                    "udf4",
                    requestDto.getPropertyId() != null
                            ? String.valueOf(requestDto.getPropertyId())
                            : ""
            );
            payload.put("metaInfo", metaInfo);

            /*
              Production safer config:
              UPI_INTENT only force केलेलं नाही.
              PhonePe available modes स्वतः handle करेल.
            */
            Map<String, Object> paymentFlow = new HashMap<>();
            paymentFlow.put("type", "PG_CHECKOUT");
            payload.put("paymentFlow", paymentFlow);

            String jsonPayload =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(payload);

            System.out.println("========== PhonePe Mobile SDK Create Order Request ==========");
            System.out.println("URL: " + phonePeConfig.getSdkOrderUrl());
            System.out.println("MerchantOrderId: " + merchantOrderId);
            System.out.println("PremiumFor: " + premiumFor);
            System.out.println("Amount Paise: " + PREMIUM_AMOUNT_IN_PAISA);
            System.out.println("MerchantId from config: " + phonePeConfig.getMerchantId());
            System.out.println("Environment from config: " + phonePeConfig.getEnvironment());
            System.out.println("Payload: " + jsonPayload);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "O-Bearer " + accessToken);

            HttpEntity<String> request =
                    new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    phonePeConfig.getSdkOrderUrl(),
                    request,
                    Map.class
            );

            System.out.println("========== PhonePe Mobile SDK Create Order Response ==========");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());

            Map body = response.getBody();

            if (body == null) {
                throw new RuntimeException("PhonePe Mobile SDK response body is null");
            }

            String phonePeOrderId =
                    body.get("orderId") != null
                            ? body.get("orderId").toString()
                            : null;

            String token =
                    body.get("token") != null
                            ? body.get("token").toString()
                            : null;

            if (token == null || token.isBlank()) {
                throw new RuntimeException(
                        "PhonePe Mobile SDK did not return token. Response: " + body
                );
            }

            PaymentTransaction txn = new PaymentTransaction();
            txn.setMerchantOrderId(merchantOrderId);
            txn.setOrderId(merchantOrderId);
            txn.setTransactionId(phonePeOrderId);
            txn.setPaymentStatus(PaymentStatus.PENDING);
            txn.setPremiumFor(premiumFor);
            txn.setUserId(requestDto.getUserId());
            txn.setOwnerId(requestDto.getOwnerId());
            txn.setPropertyId(requestDto.getPropertyId());
            txn.setAmount(PREMIUM_TOTAL_AMOUNT);
            txn.setBaseAmount(PREMIUM_BASE_AMOUNT);
            txn.setGstAmount(PREMIUM_GST_AMOUNT);
            txn.setTotalAmount(PREMIUM_TOTAL_AMOUNT);
            txn.setPaymentGateway("PhonePe_Mobile_SDK");
            txn.setCreatedAt(LocalDateTime.now());
            paymentRepo.save(txn);

            PhonePeMobileCreateOrderResponseDto responseDto =
                    new PhonePeMobileCreateOrderResponseDto();

            responseDto.setMerchantOrderId(merchantOrderId);
            responseDto.setOrderId(phonePeOrderId);
            responseDto.setToken(token);
            responseDto.setMerchantId(phonePeConfig.getMerchantId());
            responseDto.setEnvironment(phonePeConfig.getEnvironment());

            return responseDto;

        } catch (Exception e) {
            System.err.println(
                    "PhonePe Mobile SDK create order error: " + e.getMessage()
            );
            e.printStackTrace();

            throw new RuntimeException(
                    "Error during PhonePe Mobile SDK order creation: " + e.getMessage(),
                    e
            );
        }
    }

    // ================= MOBILE SDK: Verify Payment =================

    @Override
    public Object verifyMobilePayment(String merchantOrderId) {
        try {
            PaymentTransaction txn =
                    paymentRepo.findByMerchantOrderId(merchantOrderId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Transaction not found for merchantOrderId: "
                                                    + merchantOrderId
                                    )
                            );

            String accessToken = getAccessToken();

            String statusUrl =
                    phonePeConfig.getStatusUrl()
                            .replace("{merchantOrderId}", merchantOrderId);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "O-Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            System.out.println("========== PhonePe Mobile SDK Verify Payment ==========");
            System.out.println("URL: " + statusUrl);
            System.out.println("MerchantOrderId: " + merchantOrderId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    statusUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            System.out.println("========== PhonePe Mobile SDK Status Response ==========");
            System.out.println("Response: " + response.getBody());

            Map body = response.getBody();

            if (body == null) {
                return Map.of(
                        "success", false,
                        "status", "ERROR",
                        "message", "Empty response from PhonePe",
                        "merchantOrderId", merchantOrderId
                );
            }

            txn.setPaymentResponse(body.toString());

            String state =
                    body.get("state") == null
                            ? ""
                            : body.get("state").toString();

            String transactionId = extractTransactionId(body);

            if ("COMPLETED".equalsIgnoreCase(state)) {
                return handleMobilePaymentSuccess(
                        txn,
                        merchantOrderId,
                        transactionId
                );
            }

            /*
              User requirement:
              APK मध्ये PENDING दाखवायचं नाही.
              COMPLETED नसल्यास admin request जाणार नाही.
              FAILED असेल तर FAILED, बाकी PENDING/INTERRUPTED/unknown
              user closed/cancel case म्हणून CANCELLED return.
            */
            String publicStatus =
                    "FAILED".equalsIgnoreCase(state)
                            ? "FAILED"
                            : "CANCELLED";

            return handleMobilePaymentFailedOrCancelled(
                    txn,
                    merchantOrderId,
                    transactionId,
                    publicStatus,
                    state
            );

        } catch (Exception e) {
            System.err.println(
                    "PhonePe Mobile SDK verify payment error: " + e.getMessage()
            );
            e.printStackTrace();

            return Map.of(
                    "success", false,
                    "status", "ERROR",
                    "message", "Payment verification failed: " + e.getMessage(),
                    "merchantOrderId", merchantOrderId
            );
        }
    }

    // ================= PRIVATE HELPERS =================

    private String normalizePremiumFor(String premiumFor) {
        if (premiumFor == null || premiumFor.isBlank()) {
            throw new RuntimeException("premiumFor is required");
        }

        String value = premiumFor.trim().toUpperCase();

        if (!"USER".equals(value) && !"OWNER".equals(value)) {
            throw new RuntimeException("Invalid premiumFor: " + premiumFor);
        }

        return value;
    }

    private void validateMobileCreateOrderRequest(
            PhonePeMobileCreateOrderRequestDto requestDto,
            String premiumFor
    ) {
        if ("OWNER".equalsIgnoreCase(premiumFor)) {
            validateOwnerPaymentRequest(requestDto);
            return;
        }

        if ("USER".equalsIgnoreCase(premiumFor)) {
            validateUserPaymentRequest(requestDto);
        }
    }

    private void validateOwnerPaymentRequest(
            PhonePeMobileCreateOrderRequestDto requestDto
    ) {
        if (requestDto.getOwnerId() == null) {
            throw new RuntimeException("Owner ID is required");
        }

        if (requestDto.getPropertyId() == null) {
            throw new RuntimeException("Property ID is required");
        }

        PropertyOwner owner =
                propertyOwnerRepository.findById(requestDto.getOwnerId())
                        .orElseThrow(() ->
                                new RuntimeException("Owner not found")
                        );

        Property property =
                propertyRepository.findById(requestDto.getPropertyId())
                        .orElseThrow(() ->
                                new RuntimeException("Property not found")
                        );

        if (property.getPropertyOwner() == null ||
                !Objects.equals(
                        property.getPropertyOwner().getOwnerId(),
                        owner.getOwnerId()
                )) {
            throw new RuntimeException("Property does not belong to this owner");
        }

        /*
          First free property / active property साठी payment allow नाही.
          First free logic PropertyServiceImpl.addProperty मध्ये backend वरच होणार.
        */
        if (property.getPremiumStatus() == PremiumStatus.FREE_ACTIVE ||
                property.getPremiumStatus() == PremiumStatus.ACTIVE ||
                Boolean.TRUE.equals(property.isPremiumActive())) {
            throw new RuntimeException(
                    "This property is already active. Payment not required."
            );
        }

        if (property.getPremiumStatus() == PremiumStatus.PENDING_APPROVAL) {
            throw new RuntimeException(
                    "Premium request already sent for admin approval."
            );
        }
    }

    private void validateUserPaymentRequest(
            PhonePeMobileCreateOrderRequestDto requestDto
    ) {
        if (requestDto.getUserId() == null) {
            throw new RuntimeException("User ID is required");
        }

        User user =
                userRepository.findById(requestDto.getUserId())
                        .orElseThrow(() ->
                                new RuntimeException("User not found")
                        );

        if (Boolean.TRUE.equals(user.isPremiumActive())) {
            throw new RuntimeException("User premium is already active");
        }
    }

    private String extractTransactionId(Map body) {
        try {
            if (body.get("paymentDetails") instanceof List<?>) {
                List<?> paymentDetails = (List<?>) body.get("paymentDetails");

                if (!paymentDetails.isEmpty()
                        && paymentDetails.get(0) instanceof Map<?, ?>) {

                    Map<?, ?> firstPayment =
                            (Map<?, ?>) paymentDetails.get(0);

                    Object txnIdObj =
                            firstPayment.get("transactionId");

                    if (txnIdObj != null) {
                        return txnIdObj.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Map<String, Object> handleMobilePaymentSuccess(
            PaymentTransaction txn,
            String merchantOrderId,
            String transactionId
    ) {
        txn.setPaymentStatus(PaymentStatus.SUCCESS);

        if (transactionId != null && !transactionId.isBlank()) {
            txn.setTransactionId(transactionId);
        }

        txn.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(txn);

        if ("OWNER".equalsIgnoreCase(txn.getPremiumFor())) {
            handleOwnerMobilePaymentSuccess(txn, merchantOrderId, transactionId);
        } else if ("USER".equalsIgnoreCase(txn.getPremiumFor())) {
            handleUserMobilePaymentSuccess(txn, transactionId);
        }

        return new HashMap<String, Object>() {{
            put("success", true);
            put("status", "SUCCESS");
            put(
                    "message",
                    "Payment successful. Premium request sent for admin approval."
            );
            put("merchantOrderId", merchantOrderId);
        }};
    }

    private void handleOwnerMobilePaymentSuccess(
            PaymentTransaction txn,
            String merchantOrderId,
            String transactionId
    ) {
        if (txn.getPropertyId() != null) {
            Property property =
                    propertyRepository.findById(txn.getPropertyId())
                            .orElse(null);

            if (property != null) {
                property.setPaymentStatus("SUCCESS");
                property.setPremiumStatus(PremiumStatus.PENDING_APPROVAL);
                property.setPremiumActive(false);
                property.setPaymentDate(LocalDateTime.now());
                property.setPaymentOrderId(merchantOrderId);

                if (transactionId != null && !transactionId.isBlank()) {
                    property.setPaymentTransactionId(transactionId);
                }

                propertyRepository.save(property);
            }
        }

        if (txn.getOwnerId() != null) {
            PropertyOwner owner =
                    propertyOwnerRepository.findById(txn.getOwnerId())
                            .orElse(null);

            if (owner != null) {
                owner.setPaymentStatus("SUCCESS");
                owner.setPremiumStatus("PENDING_APPROVAL");
                owner.setPremiumActive(false);

                if (transactionId != null && !transactionId.isBlank()) {
                    owner.setPhonePeTransactionId(transactionId);
                }

                propertyOwnerRepository.save(owner);
            }
        }
    }

    private void handleUserMobilePaymentSuccess(
            PaymentTransaction txn,
            String transactionId
    ) {
        if (txn.getUserId() == null) {
            return;
        }

        User user =
                userRepository.findById(txn.getUserId())
                        .orElse(null);

        if (user != null) {
            user.setPaymentStatus("SUCCESS");
            user.setPremiumStatus("PENDING");
            user.setPremiumActive(false);

            if (transactionId != null && !transactionId.isBlank()) {
                user.setPhonePeTransactionId(transactionId);
            }

            userRepository.save(user);
        }
    }

    private Map<String, Object> handleMobilePaymentFailedOrCancelled(
            PaymentTransaction txn,
            String merchantOrderId,
            String transactionId,
            String publicStatus,
            String phonePeState
    ) {
        txn.setPaymentStatus(PaymentStatus.FAILED);

        if (transactionId != null && !transactionId.isBlank()) {
            txn.setTransactionId(transactionId);
        }

        txn.setPaymentResponse(
                "PhonePe state: " + phonePeState
                        + ", treated as: " + publicStatus
        );
        txn.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(txn);

        if ("OWNER".equalsIgnoreCase(txn.getPremiumFor())) {
            handleOwnerPaymentFailedOrCancelled(txn, publicStatus);
        } else if ("USER".equalsIgnoreCase(txn.getPremiumFor())) {
            handleUserPaymentFailedOrCancelled(txn, publicStatus);
        }

        String message =
                "FAILED".equalsIgnoreCase(publicStatus)
                        ? "Payment failed. Please try again."
                        : "Payment cancelled or not completed.";

        return new HashMap<String, Object>() {{
            put("success", false);
            put("status", publicStatus);
            put("message", message);
            put("merchantOrderId", merchantOrderId);
        }};
    }

    private void handleOwnerPaymentFailedOrCancelled(
            PaymentTransaction txn,
            String publicStatus
    ) {
        if (txn.getPropertyId() != null) {
            Property property =
                    propertyRepository.findById(txn.getPropertyId())
                            .orElse(null);

            if (property != null) {
                property.setPaymentStatus(publicStatus);
                property.setPremiumStatus(PremiumStatus.NONE);
                property.setPremiumActive(false);
                propertyRepository.save(property);
            }
        }

        if (txn.getOwnerId() != null) {
            PropertyOwner owner =
                    propertyOwnerRepository.findById(txn.getOwnerId())
                            .orElse(null);

            if (owner != null) {
                owner.setPaymentStatus(publicStatus);
                owner.setPremiumActive(false);

                /*
                  Owner free status disturb करू नये.
                  Only paid premium request fail/cancel status save.
                */
                if (!"FREE_ACTIVE".equalsIgnoreCase(owner.getPremiumStatus())) {
                    owner.setPremiumStatus(publicStatus);
                }

                propertyOwnerRepository.save(owner);
            }
        }
    }

    private void handleUserPaymentFailedOrCancelled(
            PaymentTransaction txn,
            String publicStatus
    ) {
        if (txn.getUserId() == null) {
            return;
        }

        User user =
                userRepository.findById(txn.getUserId())
                        .orElse(null);

        if (user != null) {
            user.setPaymentStatus(publicStatus);
            user.setPremiumStatus(publicStatus);
            user.setPremiumActive(false);
            userRepository.save(user);
        }
    }
}
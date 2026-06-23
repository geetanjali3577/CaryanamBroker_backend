package com.caryanam.caryanam_broker.serviceimpl;

import com.caryanam.caryanam_broker.configuration.PhonePeConfig;
import com.caryanam.caryanam_broker.dto.PremiumPaymentResponseDto;
import com.caryanam.caryanam_broker.Enum.PaymentStatus;
import com.caryanam.caryanam_broker.entity.PaymentTransaction;
import com.caryanam.caryanam_broker.entity.User;
import com.caryanam.caryanam_broker.repository.PaymentTransactionRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import com.caryanam.caryanam_broker.service.PhonePeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhonePeServiceImpl implements PhonePeService {
    @Autowired
    private PhonePeConfig phonePeConfig;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private PaymentTransactionRepository paymentRepo;

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

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            System.out.println("========== PhonePe V2 OAuth Token Request ==========");
            System.out.println("URL: " + phonePeConfig.getAuthUrl());
            System.out.println("ClientId: " + phonePeConfig.getClientId());

            System.out.println("AUTH URL = " + phonePeConfig.getAuthUrl());
            System.out.println("CLIENT ID = " + phonePeConfig.getClientId());

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

        } catch (Exception e) {
            System.err.println("PhonePe OAuth token error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get PhonePe OAuth access token: " + e.getMessage(), e);
        }
    }

    // ================= V2 PG_CHECKOUT: Create Premium Order =================
    @Override
    public PremiumPaymentResponseDto createPremiumOrder(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        String orderId = "PREMIUM_" + System.currentTimeMillis();

        // TEST AMOUNT ₹1
//        double baseAmount = 1.0;
//        double gstAmount = 0.0;
//        double totalAmount = 1.0;
//        long phonePeAmount = 100L; // ₹1 = 100 paisa
        double baseAmount = 99.0;
        double gstAmount = 17.82;
        double totalAmount = 116.82;
        long phonePeAmount = Math.round(totalAmount * 100);

        // Set user state to PAYMENT_PENDING before calling PhonePe
        user.setPhonePeOrderId(orderId);
        user.setPaymentStatus("PENDING");
        user.setPremiumStatus("PAYMENT_PENDING");
        user.setPremiumAmount(totalAmount);
        user.setPremiumActive(false);
        user.setPremiumCount(user.getPremiumCount() + 1);
        userRepository.save(user);

        // Create PaymentTransaction as PENDING
        PaymentTransaction txn = new PaymentTransaction();
        txn.setUserId(userId);
        txn.setOrderId(orderId);
        txn.setAmount(totalAmount);
        txn.setBaseAmount(baseAmount);
        txn.setGstAmount(gstAmount);
        txn.setTotalAmount(totalAmount);
        txn.setPaymentGateway("PhonePe");
        txn.setPaymentStatus(PaymentStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(txn);

        try {
            // Step 1: Get OAuth access token
            String accessToken = getAccessToken();

            // Step 2: Build V2 PG_CHECKOUT payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("merchantOrderId", orderId);
            payload.put("amount", phonePeAmount);
            payload.put("expireAfter", 1200); // 20 minutes

            Map<String, Object> paymentFlow = new HashMap<>();
            Map<String, Object> payPageConfig = new HashMap<>();
            payPageConfig.put("redirectUrl", phonePeConfig.getRedirectUrl());
            paymentFlow.put("paymentFlow", Map.of("type", "PG_CHECKOUT", "merchantUrls", payPageConfig));

            // Merge paymentFlow into payload
            payload.put("paymentFlow", paymentFlow.get("paymentFlow"));

            String jsonPayload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);

            System.out.println("========== PhonePe V2 Pay Request ==========");
            System.out.println("URL: " + phonePeConfig.getPayUrl());
            System.out.println("OrderId: " + orderId);
            System.out.println("Amount (paise): " + phonePeAmount);
            System.out.println("Payload: " + jsonPayload);

            // Step 3: Call PhonePe V2 checkout API
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
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

            Map body = response.getBody();
            if (body == null) {
                throw new RuntimeException("PhonePe V2 response body is null");
            }

            // V2 response: { "orderId": "...", "state": "PENDING", "redirectUrl": "https://..." }
            String redirectUrl = null;
            if (body.containsKey("redirectUrl")) {
                redirectUrl = body.get("redirectUrl").toString();
            }

            if (redirectUrl == null || redirectUrl.isBlank()) {
                throw new RuntimeException("PhonePe V2 did not return a redirectUrl. Response: " + body);
            }

            // Save PhonePe's orderId if available
            if (body.containsKey("orderId")) {
                txn.setTransactionId(body.get("orderId").toString());
                paymentRepo.save(txn);
            }

            return new PremiumPaymentResponseDto(
                    orderId,
                    totalAmount,
                    redirectUrl,
                    "PENDING"
            );

        } catch (Exception e) {
            e.printStackTrace();

            // Mark payment as FAILED on any error
            txn.setPaymentStatus(PaymentStatus.FAILED);
            txn.setPaymentResponse("PhonePe V2 payment initiation failed: " + e.getMessage());
            paymentRepo.save(txn);

            user.setPaymentStatus("FAILED");
            user.setPremiumStatus("REJECTED");
            userRepository.save(user);

            throw new RuntimeException("Error during PhonePe V2 order creation: " + e.getMessage(), e);
        }
    }

    @Override
    public void paymentSuccess(
            String orderId,
            String transactionId) {

        PaymentTransaction txn =
                paymentRepo.findByOrderId(orderId)
                        .orElseThrow();

        txn.setPaymentStatus(PaymentStatus.SUCCESS);
        txn.setTransactionId(transactionId);
        paymentRepo.save(txn);

        User user =
                userRepository.findById(txn.getUserId())
                        .orElseThrow();
        user.setPaymentStatus("SUCCESS");
        user.setPhonePeTransactionId(transactionId);

        userRepository.save(user);
    }

    @Override
    public void propertyPaymentSuccess(String orderId, String transactionId) {
        // Property payment handled by PremiumSubscriptionController webhook
    }
}

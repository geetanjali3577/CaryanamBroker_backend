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
//    @Override
//    public PremiumPaymentResponseDto createPremiumOrder(Long userId) {
//        String accessToken = getAccessToken();
//
//        System.out.println("PHONEPE TOKEN = " + accessToken);
//        User user =
//                userRepository.findById(userId)
//                        .orElseThrow();
//
//        String orderId =
//                "PREMIUM_" + System.currentTimeMillis();
//
//        user.setPhonePeOrderId(orderId);
//
//        user.setPaymentStatus("PENDING");
//
//        user.setPremiumAmount(116.82);
//
//        userRepository.save(user);
//
//        PaymentTransaction txn =
//                new PaymentTransaction();
//
//        txn.setUserId(userId);
//
//        txn.setOrderId(orderId);
//
//        txn.setAmount(116.82);
//        txn.setBaseAmount(99.0);
//        txn.setGstAmount(17.82);
//        txn.setTotalAmount(116.82);
//
//        txn.setPaymentStatus(PaymentStatus.PENDING);
//
//        txn.setCreatedAt(LocalDateTime.now());
//
//        paymentRepo.save(txn);
//
////        return new PremiumPaymentResponseDto(
////                orderId,
////                116.82,
////                null,
////                "PENDING"
////        );
//        return new PremiumPaymentResponseDto(
//                orderId,
//                116.82,
//                "https://business.phonepe.com",
//                "PENDING"
//        );
//    }
//---------------------------------------------------------new Method-----------------------
@Override
public PremiumPaymentResponseDto createPremiumOrder(Long userId) {

    String accessToken = getAccessToken();

    User user = userRepository.findById(userId)
            .orElseThrow();

    String orderId = "PREMIUM_" + System.currentTimeMillis();

    // TEST AMOUNT
    double baseAmount = 1.0;
    double gstAmount = 0.0;
    double totalAmount = 1.0;
    long phonePeAmount = 100L; // ₹1

    user.setPhonePeOrderId(orderId);
    user.setPaymentStatus("PENDING");
    user.setPremiumAmount(totalAmount);
    userRepository.save(user);

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
    metaInfo.put("udf1", "USER_PREMIUM");
    metaInfo.put("udf2", String.valueOf(userId));

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("merchantOrderId", orderId);
    requestBody.put("amount", phonePeAmount);
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

    if (body == null || body.get("redirectUrl") == null) {
        throw new RuntimeException("PhonePe redirectUrl not received: " + body);
    }

    String redirectUrl = body.get("redirectUrl").toString();

    return new PremiumPaymentResponseDto(
            orderId,
            totalAmount,
            redirectUrl,
            "PENDING"
    );
}
//---------------------------------------------------
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
//
//        user.setPremiumActive(true);
//        user.setPremiumStatus("APPROVED");
//        user.setPropertyLimit(100);

        userRepository.save(user);
    }

    @Override
    public void propertyPaymentSuccess(String orderId, String transactionId) {

    }

    //----------------------------------------Phone Pay Token--------------------------
//    @Override
//    public String getAccessToken() {
//
//        System.out.println("========== PHONEPE ==========");
//        System.out.println("CLIENT ID : " + phonePeConfig.getClientId());
//        System.out.println("CLIENT SECRET : " + phonePeConfig.getClientSecret());
//
//        String url =
//                "https://api-preprod.phonepe.com/apis/identity-manager/v1/oauth/token";
//
//        RestTemplate restTemplate = new RestTemplate();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        MultiValueMap<String, String> body =
//                new LinkedMultiValueMap<>();
//FV
//        body.add("client_id",
//                phonePeConfig.getClientId());
//
//        body.add("client_secret",
//                phonePeConfig.getClientSecret());
//
//        body.add("grant_type",
//                "client_credentials");
//
//        HttpEntity<MultiValueMap<String, String>> request =
//                new HttpEntity<>(body, headers);
//
//        try {
//
//            ResponseEntity<String> response =
//                    restTemplate.postForEntity(
//                            url,
//                            request,
//                            String.class);
//
//            System.out.println("========== RESPONSE ==========");
//            System.out.println(response.getBody());
//
//            return response.getBody();
//
//        } catch (Exception e) {
//
//            System.out.println("========== ERROR ==========");
//            e.printStackTrace();
//
//            return e.getMessage();
//        }
//    }

    @Override
    public String getAccessToken() {

        String url =
                "https://api.phonepe.com/apis/identity-manager/v1/oauth/token";

        RestTemplate restTemplate =
                new RestTemplate();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add("client_id",
                phonePeConfig.getClientId());

        body.add("client_version",
                String.valueOf(
                        phonePeConfig.getClientVersion()));

        body.add("client_secret",
                phonePeConfig.getClientSecret());

        body.add("grant_type",
                "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        request,
                        Map.class);

        return response.getBody()
                .get("access_token")
                .toString();
    }
}

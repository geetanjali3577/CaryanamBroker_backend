//package com.caryanam.caryanam_broker.controller;
//
//import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderRequestDto;
//import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderResponseDto;
//import com.caryanam.caryanam_broker.service.PhonePeService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/phonepe")
//public class PhonePeMobileController {
//
//    @Autowired
//    private PhonePeService phonePeService;
//
//    // ================= MOBILE SDK: Create Order =================
//    // POST /api/phonepe/mobile/create-order
//    @PostMapping("/mobile/create-order")
//    public ResponseEntity<Object> createMobileOrder(
//            @RequestBody PhonePeMobileCreateOrderRequestDto requestDto) {
//        try {
//            PhonePeMobileCreateOrderResponseDto response =
//                    phonePeService.createMobileOrder(requestDto);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of(
//                            "success", false,
//                            "message", "Mobile order creation failed",
//                            "error", e.getMessage()
//                    ));
//        }
//    }
//
//    // ================= MOBILE SDK: Verify Payment =================
//    // GET /api/phonepe/mobile/verify/{merchantOrderId}
//    @GetMapping("/mobile/verify/{merchantOrderId}")
//    public ResponseEntity<Object> verifyMobilePayment(
//            @PathVariable String merchantOrderId) {
//        try {
//            Object response = phonePeService.verifyMobilePayment(merchantOrderId);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of(
//                            "success", false,
//                            "message", "Payment verification failed",
//                            "error", e.getMessage()
//                    ));
//        }
//    }
//}



package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderRequestDto;
import com.caryanam.caryanam_broker.dto.PhonePeMobileCreateOrderResponseDto;
import com.caryanam.caryanam_broker.service.PhonePeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/phonepe")
public class PhonePeMobileController {

    @Autowired
    private PhonePeService phonePeService;

    // ================= MOBILE SDK: Create Order =================
    // POST /api/phonepe/mobile/create-order
    @PostMapping("/mobile/create-order")
    public ResponseEntity<Object> createMobileOrder(
            @RequestBody PhonePeMobileCreateOrderRequestDto requestDto) {
        try {
            PhonePeMobileCreateOrderResponseDto response =
                    phonePeService.createMobileOrder(requestDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "status", "ERROR",
                            "message", "Mobile order creation failed",
                            "error", e.getMessage()
                    ));
        }
    }

    // ================= MOBILE SDK: Verify Payment =================
    // GET /api/phonepe/mobile/verify/{merchantOrderId}
    @GetMapping("/mobile/verify/{merchantOrderId}")
    public ResponseEntity<Object> verifyMobilePayment(
            @PathVariable String merchantOrderId) {
        try {
            Object response =
                    phonePeService.verifyMobilePayment(merchantOrderId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "status", "ERROR",
                            "message", "Payment verification failed",
                            "error", e.getMessage()
                    ));
        }
    }
}
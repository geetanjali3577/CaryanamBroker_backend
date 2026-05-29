package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.service.AreaPincodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/area")
public class AreaPincodeController {

    @Autowired
    private AreaPincodeService areaPincodeService;

    @PostMapping("/uploadExcel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        String response = areaPincodeService.uploadExcel(file);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyData(
            @RequestParam String nearbyPincode) {

        return ResponseEntity.ok(
                areaPincodeService.getNearbyData(nearbyPincode)
        );
    }
}
package com.caryanam.caryanam_broker.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AreaPincodeService {

    String uploadExcel(MultipartFile file);
    List<String> getNearbyData(String nearbyPincode);

}
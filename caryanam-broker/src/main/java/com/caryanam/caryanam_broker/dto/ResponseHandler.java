package com.caryanam.caryanam_broker.dto;



import com.caryanam.caryanam_broker.appconstant.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class ResponseHandler {
    public static ResponseEntity<Object> generateResponse(Object message, HttpStatus status, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put(AppConstants.MESSAGE, message);
        map.put(AppConstants.STATUS, status.value());
        map.put(AppConstants.DATA, data);

        return ResponseEntity.ok(map);
    }

}

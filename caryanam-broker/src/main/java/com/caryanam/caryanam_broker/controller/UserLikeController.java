package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.configuration.CustomUserDetails;
import com.caryanam.caryanam_broker.dto.ResponseHandler;
import com.caryanam.caryanam_broker.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserLikeController {

    @Autowired
    private PropertyService propertyService;

    private Long getLoggedInUserId() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails) {

            return ((CustomUserDetails) principal).getId();
        }

        return null;
    }

    @PostMapping("/likeProperty/{propertyId}")
    public ResponseEntity<Object> likeProperty(
            @PathVariable Long propertyId) {

        Long userId = getLoggedInUserId();

        if (userId == null) {

            return ResponseHandler.generateResponse(
                    "Unauthorized",
                    HttpStatus.UNAUTHORIZED,
                    null
            );
        }

        return ResponseHandler.generateResponse(
                propertyService.toggleLikeProperty(propertyId, userId),
                HttpStatus.OK,
                null
        );
    }

    @GetMapping("/likedProperties")
    public ResponseEntity<Object> getLikedProperties() {

        Long userId = getLoggedInUserId();

        return ResponseHandler.generateResponse(
                "Liked Properties",
                HttpStatus.OK,
                propertyService.getLikedProperties(userId)
        );
    }

    @GetMapping("/likedPropertiesCount")
    public ResponseEntity<Object> getLikedPropertiesCount() {

        Long userId = getLoggedInUserId();

        return ResponseHandler.generateResponse(
                "Liked Properties Count",
                HttpStatus.OK,
                propertyService.getUserLikedPropertiesCount(userId)
        );
    }
}

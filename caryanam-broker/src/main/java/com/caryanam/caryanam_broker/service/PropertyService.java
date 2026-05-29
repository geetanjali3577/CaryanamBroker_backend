package com.caryanam.caryanam_broker.service;


import com.caryanam.caryanam_broker.dto.PropertyDto;
import com.caryanam.caryanam_broker.dto.PropertyFilterDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface PropertyService {

    record PropertyImageContent(byte[] data, String contentType) {}

    PropertyDto addProperty(PropertyDto propertyDto, Long adminId);

    List<PropertyDto> getAllProperties(Long userId, HttpServletRequest request);

    PropertyDto getPropertyById(Long id);

    PropertyDto updateProperty(Long id, PropertyDto propertyDto);

    String deleteProperty(Long id);

    String uploadPropertyImages(Long propertyId, MultipartFile[] files);

    List<?> filterProperties(PropertyFilterDto filterDto, Long userId);

    Object getPropertiesByCityAndAddress(String city, String address);
    List<PropertyDto> getPropertiesByOwnerId(Long ownerId);

    String activateProperty(Long id);

    Optional<PropertyImageContent> getPropertyImageContent(String filename);
    //........................................................
    String toggleLikeProperty(Long propertyId, Long userId);

    List<PropertyDto> getLikedProperties(Long userId);

    Integer getUserLikedPropertiesCount(Long userId);

}

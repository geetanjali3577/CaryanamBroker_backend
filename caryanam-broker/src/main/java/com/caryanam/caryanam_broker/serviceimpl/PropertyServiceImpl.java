//package com.caryanam.caryanam_broker.serviceimpl;
//
//import com.caryanam.caryanam_broker.appconstant.AppConstants;
//import com.caryanam.caryanam_broker.dto.PropertyDto;
//import com.caryanam.caryanam_broker.dto.PropertyFilterDto;
//import com.caryanam.caryanam_broker.entity.*;
//import com.caryanam.caryanam_broker.enums.PremiumStatus;
//import com.caryanam.caryanam_broker.messageconfig.MessageConfig;
//import com.caryanam.caryanam_broker.repository.*;
//import com.caryanam.caryanam_broker.service.AreaPincodeService;
//import com.caryanam.caryanam_broker.service.PropertyService;
//import jakarta.servlet.http.HttpServletRequest;
//import net.coobird.thumbnailator.Thumbnails;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.ByteArrayOutputStream;
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Base64;
//import java.util.List;
//import java.util.Optional;
//
//
//@Service
//public class PropertyServiceImpl implements PropertyService {
//
//    @Autowired
//    private PropertyRepository propertyRepository;
//
//    @Autowired
//    private PropertyImageRepository propertyImageRepository;
//
//    @Autowired
//    private PropertyLikeRepository propertyLikeRepository;
//
//    @Autowired
//    private PropertyOwnerRepository propertyOwnerRepository;
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private AreaPincodeService areaPincodeService;
//
//    @Value("${app.public-base-url:https://r1.rentalchaavi.com}")
//    private String publicBaseUrl;
//
//    @Value("${app.property-images-dir:/home/ubuntu/property-images/}")
//    private String propertyImagesDir;
//
//
//    private String getImageUrl(PropertyImage image) {
//
//        if (image == null || image.getImagePath() == null) {
//            return null;
//        }
//
//        String base = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
//        return base + "/api/owner/property/image/" + image.getImagePath();
//    }
//
//
//    private void attachDatabaseImages(PropertyDto dto, Long propertyId) {
//
//        List<PropertyImage> images =
//                propertyImageRepository.findByPropertyId(propertyId);
//
//        List<String> doctypeImageBase64List = new ArrayList<>();
//
//        List<String> doctypeImageNames = new ArrayList<>();
//
//        if (images != null && !images.isEmpty()) {
//
//            for (int i = 0; i < images.size(); i++) {
//
//                PropertyImage image = images.get(i);
//
//                String dataUrl = getImageUrl(image);
//                if (i == 0) {
//
//                    dto.setCoverImage(image.getImageName());
//
//                    dto.setCoverImageBase64(dataUrl);
//
//                } else {
//
//                    if (dataUrl != null) {
//                        doctypeImageBase64List.add(dataUrl);
//                    }
//
//                    if (image.getImageName() != null) {
//                        doctypeImageNames.add(image.getImageName());
//                    }
//                }
//            }
//        }
//
//        dto.setDoctypeImageBase64List(doctypeImageBase64List);
//
//        dto.setDoctypeImages(String.valueOf(doctypeImageNames));
//    }
//
//    private String getImageOutputFormat(String contentType, String originalName) {
//        String normalizedContentType = String.valueOf(contentType).toLowerCase();
//        String normalizedName = String.valueOf(originalName).toLowerCase();
//
//        if (normalizedContentType.contains("png") || normalizedName.endsWith(".png")) {
//            return "png";
//        }
//
//        return "jpg";
//    }
//
//    private String getResponseContentType(String outputFormat, String contentType) {
//        if ("png".equals(outputFormat)) {
//            return "image/png";
//        }
//
//        if (contentType != null && !contentType.isBlank()) {
//            return contentType;
//        }
//
//        return "image/jpeg";
//    }
//
//    private boolean matchesText(String source, String filter) {
//        return source != null
//                && filter != null
//                && source.trim().equalsIgnoreCase(filter.trim());
//    }
//
//    private boolean matchesLocationOrAddress(Property property, String filter) {
//        return matchesText(property.getLocation(), filter)
//                || matchesText(property.getAddress(), filter);
//    }
//
//
//
//    @Override
//    @Transactional
//    public PropertyDto addProperty(PropertyDto propertyDto, Long ownerId) {
//
//        PropertyOwner owner = propertyOwnerRepository.findById(ownerId).orElse(null);
//        if (owner == null) {
//            return null;
//        }
//        Property property = new Property();
//        property.setTitle(propertyDto.getTitle());
//        property.setPrice(propertyDto.getPrice());
//        property.setLocation(propertyDto.getLocation());
//        property.setAddress(propertyDto.getAddress());
//        property.setCity(propertyDto.getCity());
//        property.setState(propertyDto.getState());
//        property.setPincode(propertyDto.getPincode());
//        property.setDescription(propertyDto.getDescription());
//        property.setPropertyType(propertyDto.getPropertyType());
//        property.setPgType(propertyDto.getPgType());
//        property.setBhkType(propertyDto.getBhkType());
//        property.setFurnishing(propertyDto.getFurnishing());
////        property.setCarpetArea(propertyDto.getCarpetArea());
//        property.setCarpetArea(propertyDto.getCarpetArea().trim() + " sq ft");
//        property.setMobileNumber(propertyDto.getMobileNumber());
//        property.setApartmentName(propertyDto.getApartmentName());
//        property.setLikesCount(0);
//        property.setViewsCount(0);
//
//        //changesforphonepe
//        if (Boolean.TRUE.equals(owner.getFreeOwner()) && !Boolean.TRUE.equals(owner.getFreePropertyUsed())) {
//            property.setPremiumStatus(PremiumStatus.FREE_ACTIVE);
//            property.setPremiumStartDate(java.time.LocalDateTime.now());
//            property.setPremiumEndDate(java.time.LocalDateTime.now().plusDays(30));
//            property.setIsFirstFreeProperty(true);
//            property.setPremiumActive(true);
//            property.setStatus(AppConstants.ACTIVE);
//
//            owner.setFreePropertyUsed(true);
//            owner.setPremiumActive(true);
//            owner.setPremiumStatus("FREE_ACTIVE");
//            propertyOwnerRepository.save(owner);
//        } else {
//            property.setPremiumStatus(PremiumStatus.NONE);
//            property.setPaymentStatus("UNPAID");
//            property.setIsFirstFreeProperty(false);
//            property.setPremiumActive(false);
//            property.setStatus(AppConstants.PENDING);
//        }
//
//        property.setPropertyOwner(owner);
//        Property saved = propertyRepository.save(property);
//        PropertyDto dto = new PropertyDto();
//        dto.setId(saved.getId());
//        dto.setTitle(saved.getTitle());
//        dto.setPrice(saved.getPrice());
//        dto.setStatus(saved.getStatus());
//
//        return dto;
//    }
//
//
//    @Override
//    public List<PropertyDto> getAllProperties(Long userId, HttpServletRequest request) {
//
//        boolean isPremium = false;
//
//        if (request.getAttribute("isPremium") != null) {
//            isPremium = (boolean) request.getAttribute("isPremium");
//        }
//
//        // ONLY ACTIVE + APPROVED PAYMENT STATUS
//        List<Property> properties =
//                propertyRepository.findByStatus(AppConstants.ACTIVE);
//
//        List<PropertyDto> dtoList = new ArrayList<>();
//
//        for (Property property : properties) {
//
//            // PAYMENT STATUS CHECK
//            // PENDING PAYMENT PROPERTIES HIDE
//            if (property.getPremiumStatus() != PremiumStatus.FREE_ACTIVE
//                    && property.getPremiumStatus() != PremiumStatus.ACTIVE) {
//                continue;
//            }
//
//            PropertyOwner owner = property.getPropertyOwner();
//
//            if (owner == null) {
//                continue;
//            }
//
//            PropertyDto dto = new PropertyDto();
//            attachDatabaseImages(dto, property.getId());
//
//            // NON PREMIUM USER
//            if (!isPremium) {
//
//                dto.setId(property.getId());
//                dto.setTitle(property.getTitle());
//                dto.setPrice(property.getPrice());
//                dto.setLocation(property.getLocation());
//                dto.setBhkType(property.getBhkType());
//                dto.setCity(property.getCity());
//                dto.setAddress(property.getAddress());
//                dto.setNearBy(property.getNearBy());
//                dto.setPincode(property.getPincode());
//                if (property.getPincode() != null && !property.getPincode().isBlank()) {
//                    List<String> nearbyAreas =
//                            areaPincodeService.getNearbyData(property.getPincode());
//
//                    dto.setNearBy(String.valueOf(nearbyAreas));
//                }
//            } else {
//
//                // PREMIUM USER → FULL DETAILS
//                dto.setId(property.getId());
//                dto.setTitle(property.getTitle());
//                dto.setPrice(property.getPrice());
//                dto.setLocation(property.getLocation());
//                dto.setAddress(property.getAddress());
//                dto.setCity(property.getCity());
//                dto.setState(property.getState());
//                dto.setPincode(property.getPincode());
//                dto.setDescription(property.getDescription());
//                dto.setPropertyType(property.getPropertyType());
//                dto.setPgType(property.getPgType());
//                dto.setBhkType(property.getBhkType());
//                dto.setNearBy(property.getNearBy());
//                dto.setFurnishing(property.getFurnishing());
//                dto.setCarpetArea(property.getCarpetArea());
//                dto.setMobileNumber(property.getMobileNumber());
//                dto.setLikesCount(property.getLikesCount());
//                dto.setViewsCount(property.getViewsCount());
//                dto.setApartmentName(property.getApartmentName());
//                dto.setStatus(property.getStatus());
//
//                dto.setOwnerId(owner.getOwnerId());
//                dto.setOwnerName(owner.getFullName());
//
//            }
//
//            dtoList.add(dto);
//        }
//
//        return dtoList;
//    }
//
//
//    @Override
//    public PropertyDto getPropertyById(Long id) {
//        Property property = propertyRepository.findById(id).orElse(null);
//
//        if (property == null) {
//            return null;
//        }
//        PropertyOwner owner = property.getPropertyOwner();
//        if (owner == null) {
//            return null;
//        }
//        PropertyDto dto = new PropertyDto();
//        dto.setId(property.getId());
//        dto.setTitle(property.getTitle());
//        dto.setPrice(property.getPrice());
//        dto.setLocation(property.getLocation());
//        dto.setAddress(property.getAddress());
//        dto.setCity(property.getCity());
//        dto.setState(property.getState());
//        dto.setPincode(property.getPincode());
//        dto.setDescription(property.getDescription());
//        dto.setPropertyType(property.getPropertyType());
//        dto.setPgType(property.getPgType());
//        dto.setBhkType(property.getBhkType());
//        dto.setFurnishing(property.getFurnishing());
//        dto.setCarpetArea(property.getCarpetArea());
//        dto.setMobileNumber(property.getMobileNumber());
//        dto.setLikesCount(property.getLikesCount());
//        dto.setViewsCount(property.getViewsCount());
//        dto.setApartmentName(property.getApartmentName());
//        dto.setStatus(property.getStatus());
//
//        // PROPERTY PAYMENT STATUS
//        dto.setPaymentStatus(property.getPaymentStatus());
//        dto.setPremiumActive(property.isPremiumActive());
//
//        dto.setOwnerId(owner.getOwnerId());
//
//        attachDatabaseImages(dto, id);
//
//        return dto;
//    }
//
//    @Override
//    public PropertyDto updateProperty(Long id, PropertyDto propertyDto) {
//
//        Property property = propertyRepository.findById(id).orElse(null);
//        if (property == null) {
//            return null;
//        }
//        if (propertyDto.getTitle() != null) {
//            property.setTitle(propertyDto.getTitle());
//        }
//        if (propertyDto.getPrice() != null) {
//            property.setPrice(propertyDto.getPrice());
//        }
//        if (propertyDto.getLocation() != null) {
//            property.setLocation(propertyDto.getLocation());
//        }
//        if (propertyDto.getAddress() != null) {
//            property.setAddress(propertyDto.getAddress());
//        }
//        if (propertyDto.getCity() != null) {
//            property.setCity(propertyDto.getCity());
//        }
//        if (propertyDto.getState() != null) {
//            property.setState(propertyDto.getState());
//        }
//        if (propertyDto.getPincode() != null) {
//            property.setPincode(propertyDto.getPincode());
//        }
//
//        if (propertyDto.getDescription() != null) {
//            property.setDescription(propertyDto.getDescription());
//        }
//        if (propertyDto.getPropertyType() != null) {
//            property.setPropertyType(propertyDto.getPropertyType());
//        }
//        if (propertyDto.getPgType() != null) {
//            property.setPgType(propertyDto.getPgType());
//        }
//        if (propertyDto.getBhkType() != null) {
//            property.setBhkType(propertyDto.getBhkType());
//        }
//        if (propertyDto.getFurnishing() != null) {
//            property.setFurnishing(propertyDto.getFurnishing());
//        }
//        if (propertyDto.getCarpetArea() != null) {
//            property.setCarpetArea(propertyDto.getCarpetArea());
//        }
//        if (propertyDto.getMobileNumber() != null) {
//            property.setMobileNumber(propertyDto.getMobileNumber());
//        }
//        if (propertyDto.getApartmentName() != null) {
//            property.setApartmentName(propertyDto.getApartmentName());
//        }
//        Property updatedProperty = propertyRepository.save(property);
//        PropertyDto responseDto = new PropertyDto();
//        responseDto.setId(updatedProperty.getId());
//        responseDto.setTitle(updatedProperty.getTitle());
//        responseDto.setPrice(updatedProperty.getPrice());
//        responseDto.setLocation(updatedProperty.getLocation());
//        responseDto.setAddress(updatedProperty.getAddress());
//        responseDto.setCity(updatedProperty.getCity());
//        responseDto.setState(updatedProperty.getState());
//        responseDto.setPincode(updatedProperty.getPincode());
//        responseDto.setDescription(updatedProperty.getDescription());
//        responseDto.setPropertyType(updatedProperty.getPropertyType());
//        responseDto.setPgType(updatedProperty.getPgType());
//        responseDto.setBhkType(updatedProperty.getBhkType());
//        responseDto.setFurnishing(updatedProperty.getFurnishing());
//        responseDto.setCarpetArea(updatedProperty.getCarpetArea());
//        responseDto.setMobileNumber(updatedProperty.getMobileNumber());
//        responseDto.setApartmentName(updatedProperty.getApartmentName());
//        responseDto.setStatus(updatedProperty.getStatus());
//        responseDto.setLikesCount(updatedProperty.getLikesCount());
//        responseDto.setViewsCount(updatedProperty.getViewsCount());
//        attachDatabaseImages(responseDto, updatedProperty.getId());
//
//        return responseDto;
//    }
//
//    @Override
//    public String deleteProperty(Long id) {
//        Property property = propertyRepository.findById(id).orElse(null);
//        if (property == null) {
//            return MessageConfig.PROPERTY_NOT_FOUND;
//        }
//        property.setStatus(AppConstants.INACTIVE);
//        propertyRepository.save(property);
//        return AppConstants.PROPERTY_DELETED;
//    }
//
//
//    @Override
//    public String uploadPropertyImages(Long propertyId, MultipartFile[] files) {
//
//        Property property = propertyRepository.findById(propertyId).orElse(null);
//
//        if (property == null) {
//            return MessageConfig.PROPERTY_NOT_FOUND;
//        }
//
//        int index = 0;
//
//        StringBuilder doctypeImages = new StringBuilder();
//
//        for (MultipartFile file : files) {
//
//            try {
//
//                String originalName = file.getOriginalFilename();
//
//                String extension = "";
//
//                if (originalName != null && originalName.contains(".")) {
//                    extension = originalName.substring(originalName.lastIndexOf("."));
//                }
//
//                String fileName =
//                        System.currentTimeMillis() + "_" + index + extension;
//
//                Long originalKb = file.getSize() / 1024;
//
//                Double originalMb =
//                        file.getSize() / (1024.0 * 1024.0);
//
//                PropertyImage image = new PropertyImage();
//
//                image.setImageName(fileName);
//
//                image.setImagePath(fileName);
//
//                image.setContentType(file.getContentType());
//
//                // DB IMAGE SAVE
//                image.setImageData(file.getBytes());
//
//                image.setOriginalSizeKb(originalKb);
//
//                image.setOriginalSizeMb(originalMb);
//
//                image.setCompressedSizeKb(originalKb);
//
//                image.setCompressedSizeMb(originalMb);
//
//                image.setProperty(property);
//
//                propertyImageRepository.save(image);
//
//                if (index == 0) {
//
//                    property.setCoverImage(fileName);
//
//                } else {
//
//                    if (doctypeImages.length() > 0) {
//                        doctypeImages.append(",");
//                    }
//
//                    doctypeImages.append(fileName);
//                }
//
//                index++;
//
//            } catch (Exception e) {
//
//                e.printStackTrace();
//
//                return MessageConfig.IMAGE_UPLOAD_FAILED;
//            }
//        }
//
//        if (doctypeImages.length() > 0) {
//
//            property.setDoctypeImages(doctypeImages.toString());
//        }
//
//        int totalImages =
//                propertyImageRepository.countByPropertyId(propertyId);
//
//        property.setStatus(AppConstants.ACTIVE);
//
//        propertyRepository.save(property);
//
//        if (totalImages < 4) {
//
//            return AppConstants.UPLOAD_SUCCESSFULLY
//                    + (4 - totalImages)
//                    + AppConstants.MORE_IMG;
//        }
//
//        return MessageConfig.IMAGE_UPLOAD_SUCCESS;
//    }
//
//    @Override
//    public List<?> filterProperties(PropertyFilterDto filterDto, Long userId) {
//
//        User user = userRepository.findById(userId).orElse(null);
//
//        boolean isPremium = false;
//
//        if (user != null && user.isPremiumActive()) {
//            isPremium = true;
//        }
//
//        // ONLY ACTIVE PROPERTIES
//        List<Property> allProperties =
//                propertyRepository.findByStatus(AppConstants.ACTIVE);
//
//        List<Property> filteredList = new ArrayList<>();
//
//        for (Property property : allProperties) {
//
//            // PAYMENT STATUS CHECK
//            // ONLY APPROVED PAYMENT STATUS SHOW
//            if (property.getPremiumStatus() != PremiumStatus.FREE_ACTIVE
//                    && property.getPremiumStatus() != PremiumStatus.ACTIVE) {
//                continue;
//            }
//
//            PropertyOwner owner = property.getPropertyOwner();
//
//            if (owner == null) {
//                continue;
//            }
//
//            boolean match = true;
//
//            // CITY FILTER
//            if (filterDto.getCity() != null
//                    && !filterDto.getCity().isEmpty()) {
//
//                if (!property.getCity()
//                        .equalsIgnoreCase(filterDto.getCity())) {
//                    match = false;
//                }
//            }
//
//            // ADDRESS FILTER
//            if (filterDto.getAddress() != null
//                    && !filterDto.getAddress().isBlank()) {
//
//                if (!matchesLocationOrAddress(property, filterDto.getAddress())) {
//                    match = false;
//                }
//            }
//
//            // PROPERTY TYPE FILTER
//            if (filterDto.getPropertyType() != null
//                    && !filterDto.getPropertyType().isEmpty()
//                    && !filterDto.getPropertyType().equalsIgnoreCase("ALL")) {
//
//                if (!property.getPropertyType().name()
//                        .equalsIgnoreCase(filterDto.getPropertyType())) {
//                    match = false;
//                }
//            }
//
//            // PG TYPE FILTER
//            if (filterDto.getPgType() != null
//                    && !filterDto.getPgType().isEmpty()
//                    && !filterDto.getPgType().equalsIgnoreCase("ALL")) {
//
//                if (property.getPgType() == null
//                        || !property.getPgType().name()
//                        .equalsIgnoreCase(filterDto.getPgType())) {
//
//                    match = false;
//                }
//            }
//
//            // MIN PRICE FILTER
//            if (filterDto.getMinPrice() != null
//                    && property.getPrice() < filterDto.getMinPrice()) {
//
//                match = false;
//            }
//
//            // MAX PRICE FILTER
//            if (filterDto.getMaxPrice() != null
//                    && property.getPrice() > filterDto.getMaxPrice()) {
//
//                match = false;
//            }
//
//            if (match) {
//                filteredList.add(property);
//            }
//        }
//
//        List<PropertyDto> dtoList = new ArrayList<>();
//
//        for (Property property : filteredList) {
//
//            PropertyDto dto = new PropertyDto();
//            attachDatabaseImages(dto, property.getId());
//
//            // NON PREMIUM USER
//            if (!isPremium) {
//
//                dto.setId(property.getId());
//                dto.setTitle(property.getTitle());
//                dto.setPrice(property.getPrice());
//                dto.setLocation(property.getLocation());
//                dto.setAddress(property.getAddress());
//                dto.setCity(property.getCity());
//                dto.setBhkType(property.getBhkType());
//                dto.setPropertyType(property.getPropertyType());
//                dto.setApartmentName(property.getApartmentName());
//
//            } else {
//
//                // PREMIUM USER FULL DETAILS
//                dto.setId(property.getId());
//                dto.setTitle(property.getTitle());
//                dto.setPrice(property.getPrice());
//                dto.setLocation(property.getLocation());
//                dto.setAddress(property.getAddress());
//                dto.setCity(property.getCity());
//                dto.setBhkType(property.getBhkType());
//                dto.setMobileNumber(property.getMobileNumber());
//                dto.setDescription(property.getDescription());
//                dto.setPropertyType(property.getPropertyType());
//                dto.setApartmentName(property.getApartmentName());
//            }
//
//            // OWNER DETAILS
//            if (property.getPropertyOwner() != null) {
//                dto.setOwnerId(property.getPropertyOwner().getOwnerId());
//            }
//
//            dtoList.add(dto);
//        }
//
//        return dtoList;
//    }
//    public List<PropertyDto> getPropertiesByCityAndAddress(String city, String address) {
//        List<Property> list;
//        if (address == null || address.isEmpty()) {
//            list = propertyRepository
//                    .findByCityIgnoreCaseAndStatus(city, AppConstants.ACTIVE);
//
//        } else {
//            list = propertyRepository
//                    .findByCityIgnoreCaseAndAddressIgnoreCaseAndStatus(city, address, AppConstants.ACTIVE);
//        }
//        List<PropertyDto> dtoList = new ArrayList<>();
//        for (Property property : list) {
//            PropertyDto dto = new PropertyDto();
//            dto.setId(property.getId());
//            dto.setTitle(property.getTitle());
//            dto.setPrice(property.getPrice());
//            dto.setAddress(property.getAddress());
//            dto.setCity(property.getCity());
//            dto.setMobileNumber(property.getMobileNumber());
//            dto.setBhkType(property.getBhkType());
//            dto.setLocation(property.getLocation());
//            dto.setApartmentName(property.getApartmentName());
//            attachDatabaseImages(dto, property.getId());
//            dtoList.add(dto);
//        }
//
//        return dtoList;
//    }
//
//    @Override
//    public List<PropertyDto> getPropertiesByOwnerId(Long ownerId) {
//
//        List<Property> properties =
//                propertyRepository.findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(ownerId);
//
//        List<PropertyDto> dtoList = new ArrayList<>();
//
//        for (Property property : properties) {
//
//            PropertyDto dto = new PropertyDto();
//
//            dto.setId(property.getId());
//            dto.setTitle(property.getTitle());
//            dto.setPrice(property.getPrice());
//            dto.setLocation(property.getLocation());
//            dto.setAddress(property.getAddress());
//            dto.setCity(property.getCity());
//            dto.setState(property.getState());
//            dto.setPincode(property.getPincode());
//            dto.setDescription(property.getDescription());
//            dto.setPropertyType(property.getPropertyType());
//            dto.setPgType(property.getPgType());
//            dto.setBhkType(property.getBhkType());
//            dto.setFurnishing(property.getFurnishing());
//            dto.setCarpetArea(property.getCarpetArea());
//            dto.setMobileNumber(property.getMobileNumber());
//            dto.setApartmentName(property.getApartmentName());
//            dto.setStatus(property.getStatus());
//            dto.setLikesCount(property.getLikesCount());
//            dto.setViewsCount(property.getViewsCount());
//
//            // PROPERTY PAYMENT STATUS
//            dto.setPaymentStatus(property.getPaymentStatus());
//            dto.setPremiumActive(property.isPremiumActive());
//
//            // OWNER DETAILS
//            PropertyOwner owner = property.getPropertyOwner();
//
//            if (owner != null) {
//
//                dto.setOwnerId(owner.getOwnerId());
//
//                dto.setPremiumActive(owner.isPremiumActive());
//
//                dto.setPremiumStatus(owner.getPremiumStatus());
//
//                dto.setPremiumCount(owner.getPremiumCount());
//
//            }
//
//            attachDatabaseImages(dto, property.getId());
//
//            dtoList.add(dto);
//        }
//
//        return dtoList;
//    }
//
//    @Override
//    public String activateProperty(Long id) {
//        Property property = propertyRepository.findById(id).orElse(null);
//        if (property == null) {
//            return MessageConfig.PROPERTY_NOT_FOUND;
//        }
//        property.setStatus(AppConstants.ACTIVE);
//        propertyRepository.save(property);
//        return "Property Activated Successfully";
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Optional<PropertyImageContent> getPropertyImageContent(String filename) {
//        if (filename == null || filename.isBlank()) {
//            return Optional.empty();
//        }
//
//        String decodedFilename = URLDecoder.decode(filename.trim(), StandardCharsets.UTF_8);
//        if (decodedFilename.contains("..") || decodedFilename.contains("/") || decodedFilename.contains("\\")) {
//            return Optional.empty();
//        }
//
//        try {
//            Optional<PropertyImage> imageOptional =
//                    propertyImageRepository.findFirstByImagePath(decodedFilename);
//
//            if (imageOptional.isPresent()) {
//                PropertyImage image = imageOptional.get();
//                byte[] data = image.getImageData();
//                if (data != null && data.length > 0) {
//                    return Optional.of(new PropertyImageContent(data, resolveContentType(image.getContentType(), decodedFilename)));
//                }
//            }
//        } catch (Exception ignored) {
//            // Fall through to legacy disk lookup (older uploads / schema issues).
//        }
//
//        return readLegacyDiskImage(decodedFilename);
//    }
//
//    private Optional<PropertyImageContent> readLegacyDiskImage(String filename) {
//        try {
//            Path imagePath = Path.of(propertyImagesDir).resolve(filename).normalize();
//            Path baseDir = Path.of(propertyImagesDir).toAbsolutePath().normalize();
//            if (!imagePath.toAbsolutePath().normalize().startsWith(baseDir)) {
//                return Optional.empty();
//            }
//            if (!Files.isRegularFile(imagePath)) {
//                return Optional.empty();
//            }
//            byte[] data = Files.readAllBytes(imagePath);
//            if (data.length == 0) {
//                return Optional.empty();
//            }
//            return Optional.of(new PropertyImageContent(data, resolveContentType(null, filename)));
//        } catch (Exception ignored) {
//            return Optional.empty();
//        }
//    }
//
//    private String resolveContentType(String contentType, String filename) {
//        if (contentType != null && !contentType.isBlank()) {
//            return contentType;
//        }
//        String lower = filename.toLowerCase();
//        if (lower.endsWith(".png")) {
//            return "image/png";
//        }
//        if (lower.endsWith(".webp")) {
//            return "image/webp";
//        }
//        if (lower.endsWith(".gif")) {
//            return "image/gif";
//        }
//        return "image/jpeg";
//    }
//    //................................
//    @Override
//    public String toggleLikeProperty(Long propertyId, Long userId) {
//
//        Property property = propertyRepository.findById(propertyId).orElse(null);
//
//        if (property == null) {
//            return "Property Not Found";
//        }
//
//        User user = userRepository.findById(userId).orElse(null);
//
//        if (user == null) {
//            return "User Not Found";
//        }
//
//        PropertyLike existingLike =
//                propertyLikeRepository
//                        .findByUser_UserIdAndProperty_Id(userId, propertyId)
//                        .orElse(null);
//
//        // UNLIKE
//        if (existingLike != null) {
//
//            propertyLikeRepository.delete(existingLike);
//
//            Integer count =
//                    propertyLikeRepository.countByProperty_Id(propertyId);
//
//            property.setLikesCount(count);
//
//            propertyRepository.save(property);
//
//            return "Property Unliked Successfully";
//        }
//
//        // LIKE
//        PropertyLike like = new PropertyLike();
//
//        like.setUser(user);
//
//        like.setProperty(property);
//
//        propertyLikeRepository.save(like);
//
//        Integer count =
//                propertyLikeRepository.countByProperty_Id(propertyId);
//
//        property.setLikesCount(count);
//
//        propertyRepository.save(property);
//
//        return "Property Liked Successfully";
//    }
//
//    @Override
//    public List<PropertyDto> getLikedProperties(Long userId) {
//
//        List<PropertyLike> likes =
//                propertyLikeRepository.findByUser_UserId(userId);
//
//        List<PropertyDto> dtoList = new ArrayList<>();
//
//        for (PropertyLike like : likes) {
//
//            Property property = like.getProperty();
//
//            if (property == null) {
//                continue;
//            }
//
//            PropertyDto dto = new PropertyDto();
//
//            dto.setId(property.getId());
//            dto.setTitle(property.getTitle());
//            dto.setPrice(property.getPrice());
//            dto.setLocation(property.getLocation());
//            dto.setAddress(property.getAddress());
//            dto.setCity(property.getCity());
//            dto.setLikesCount(property.getLikesCount());
//            dto.setLiked(true);
//
//
//
//            attachDatabaseImages(dto, property.getId());
//
//            dtoList.add(dto);
//        }
//
//        return dtoList;
//    }
//
//    @Override
//    public Integer getUserLikedPropertiesCount(Long userId) {
//
//        return propertyLikeRepository
//                .findByUser_UserId(userId)
//                .size();
//    }
//}
package com.caryanam.caryanam_broker.serviceimpl;

import com.caryanam.caryanam_broker.appconstant.AppConstants;
import com.caryanam.caryanam_broker.dto.PropertyDto;
import com.caryanam.caryanam_broker.dto.PropertyFilterDto;
import com.caryanam.caryanam_broker.entity.*;
import com.caryanam.caryanam_broker.enums.PremiumStatus;
import com.caryanam.caryanam_broker.messageconfig.MessageConfig;
import com.caryanam.caryanam_broker.repository.*;
import com.caryanam.caryanam_broker.service.AreaPincodeService;
import com.caryanam.caryanam_broker.service.PropertyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PropertyServiceImpl implements PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private PropertyLikeRepository propertyLikeRepository;

    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AreaPincodeService areaPincodeService;

    @Value("${app.public-base-url:https://r1.rentalchaavi.com}")
    private String publicBaseUrl;

    @Value("${app.property-images-dir:/home/ubuntu/property-images/}")
    private String propertyImagesDir;

    private String getImageUrl(PropertyImage image) {
        if (image == null || image.getImagePath() == null) {
            return null;
        }

        String base = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        return base + "/api/owner/property/image/" + image.getImagePath();
    }

    private void attachDatabaseImages(PropertyDto dto, Long propertyId) {
        List<PropertyImage> images =
                propertyImageRepository.findByPropertyId(propertyId);

        List<String> doctypeImageBase64List = new ArrayList<>();
        List<String> doctypeImageNames = new ArrayList<>();

        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                PropertyImage image = images.get(i);

                String dataUrl = getImageUrl(image);

                if (i == 0) {
                    dto.setCoverImage(image.getImageName());
                    dto.setCoverImageBase64(dataUrl);
                } else {
                    if (dataUrl != null) {
                        doctypeImageBase64List.add(dataUrl);
                    }

                    if (image.getImageName() != null) {
                        doctypeImageNames.add(image.getImageName());
                    }
                }
            }
        }

        dto.setDoctypeImageBase64List(doctypeImageBase64List);
        dto.setDoctypeImages(String.valueOf(doctypeImageNames));
    }

    private boolean matchesText(String source, String filter) {
        return source != null
                && filter != null
                && source.trim().equalsIgnoreCase(filter.trim());
    }

    private boolean matchesLocationOrAddress(Property property, String filter) {
        return matchesText(property.getLocation(), filter)
                || matchesText(property.getAddress(), filter);
    }

    private void setPropertyPremiumFields(PropertyDto dto, Property property) {
        dto.setPaymentStatus(property.getPaymentStatus());

        dto.setPremiumActive(property.isPremiumActive());

        dto.setPremiumStatus(
                property.getPremiumStatus() != null
                        ? property.getPremiumStatus().name()
                        : null
        );

        dto.setIsFirstFreeProperty(property.getIsFirstFreeProperty());
    }

    private void fillBasicPropertyDto(PropertyDto dto, Property property) {
        dto.setId(property.getId());
        dto.setTitle(property.getTitle());
        dto.setPrice(property.getPrice());
        dto.setLocation(property.getLocation());
        dto.setAddress(property.getAddress());
        dto.setCity(property.getCity());
        dto.setState(property.getState());
        dto.setPincode(property.getPincode());
        dto.setDescription(property.getDescription());
        dto.setPropertyType(property.getPropertyType());
        dto.setPgType(property.getPgType());
        dto.setBhkType(property.getBhkType());
        dto.setFurnishing(property.getFurnishing());
        dto.setCarpetArea(property.getCarpetArea());
        dto.setMobileNumber(property.getMobileNumber());
        dto.setApartmentName(property.getApartmentName());
        dto.setStatus(property.getStatus());
        dto.setRented(property.getRented());
        dto.setLikesCount(property.getLikesCount());
        dto.setViewsCount(property.getViewsCount());

        setPropertyPremiumFields(dto, property);

        if (property.getPropertyOwner() != null) {
            dto.setOwnerId(property.getPropertyOwner().getOwnerId());
        }
    }

    @Override
    @Transactional
    public PropertyDto addProperty(PropertyDto propertyDto, Long ownerId) {
        PropertyOwner owner =
                propertyOwnerRepository.findById(ownerId).orElse(null);

        if (owner == null) {
            return null;
        }

        Property property = new Property();

        property.setTitle(propertyDto.getTitle());
        property.setPrice(propertyDto.getPrice());
        property.setLocation(propertyDto.getLocation());
        property.setAddress(propertyDto.getAddress());
        property.setCity(propertyDto.getCity());
        property.setState(propertyDto.getState());
        property.setPincode(propertyDto.getPincode());
        property.setDescription(propertyDto.getDescription());
        property.setPropertyType(propertyDto.getPropertyType());
        property.setPgType(propertyDto.getPgType());
        property.setBhkType(propertyDto.getBhkType());
        property.setFurnishing(propertyDto.getFurnishing());

        if (propertyDto.getCarpetArea() != null
                && !propertyDto.getCarpetArea().isBlank()) {

            String carpetArea = propertyDto.getCarpetArea().trim();

            if (carpetArea.toLowerCase().contains("sq")) {
                property.setCarpetArea(carpetArea);
            } else {
                property.setCarpetArea(carpetArea + " sq ft");
            }
        }

        property.setMobileNumber(propertyDto.getMobileNumber());
        property.setApartmentName(propertyDto.getApartmentName());
        property.setLikesCount(0);
        property.setViewsCount(0);
        property.setRented(false);

        property.setPropertyOwner(owner);


        if (Boolean.TRUE.equals(owner.getFreeOwner())
                && !Boolean.TRUE.equals(owner.getFreePropertyUsed())) {

            property.setPremiumStatus(PremiumStatus.FREE_ACTIVE);
            property.setPaymentStatus("FREE");
            property.setPremiumStartDate(LocalDateTime.now());
            property.setPremiumEndDate(LocalDateTime.now().plusDays(30));
            property.setIsFirstFreeProperty(true);
            property.setPremiumActive(true);
            property.setStatus(AppConstants.ACTIVE);

            owner.setFreePropertyUsed(true);
            owner.setPremiumActive(true);
            owner.setPremiumStatus("FREE_ACTIVE");

            propertyOwnerRepository.save(owner);

        } else {

            property.setPremiumStatus(PremiumStatus.NONE);
            property.setPaymentStatus("UNPAID");
            property.setIsFirstFreeProperty(false);
            property.setPremiumActive(false);
            property.setStatus(AppConstants.PENDING);
        }

        Property saved = propertyRepository.save(property);

        PropertyDto dto = new PropertyDto();
        fillBasicPropertyDto(dto, saved);

        return dto;
    }

    @Override
    public List<PropertyDto> getAllProperties(Long userId, HttpServletRequest request) {
        boolean isPremium = false;

        if (request.getAttribute("isPremium") != null) {
            isPremium = (boolean) request.getAttribute("isPremium");
        }

        List<Property> properties =
                propertyRepository.findByStatus(AppConstants.ACTIVE);

        List<PropertyDto> dtoList = new ArrayList<>();

        for (Property property : properties) {
            if (property.getPremiumStatus() != PremiumStatus.FREE_ACTIVE
                    && property.getPremiumStatus() != PremiumStatus.ACTIVE) {
                continue;
            }

            PropertyOwner owner = property.getPropertyOwner();

            if (owner == null) {
                continue;
            }

            PropertyDto dto = new PropertyDto();
            attachDatabaseImages(dto, property.getId());

            if (!isPremium) {
                dto.setId(property.getId());
                dto.setTitle(property.getTitle());
                dto.setPrice(property.getPrice());
                dto.setLocation(property.getLocation());
                dto.setBhkType(property.getBhkType());
                dto.setCity(property.getCity());
                dto.setAddress(property.getAddress());
                dto.setNearBy(property.getNearBy());
                dto.setPincode(property.getPincode());
                dto.setStatus(property.getStatus());
                dto.setRented(property.getRented());

                if (property.getPincode() != null && !property.getPincode().isBlank()) {
                    List<String> nearbyAreas = areaPincodeService.getNearbyData(property.getPincode());
                    dto.setNearBy(String.valueOf(nearbyAreas));
                }

            } else {
                fillBasicPropertyDto(dto, property);
                dto.setOwnerName(owner.getFullName());
                dto.setNearBy(property.getNearBy());
            }

            setPropertyPremiumFields(dto, property);
            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    public PropertyDto getPropertyById(Long id) {
        Property property =
                propertyRepository.findById(id).orElse(null);

        if (property == null) {
            return null;
        }

        PropertyOwner owner = property.getPropertyOwner();

        if (owner == null) {
            return null;
        }

        PropertyDto dto = new PropertyDto();

        fillBasicPropertyDto(dto, property);
        dto.setOwnerId(owner.getOwnerId());

        attachDatabaseImages(dto, id);

        return dto;
    }

    @Override
    public PropertyDto updateProperty(Long id, PropertyDto propertyDto) {
        Property property =
                propertyRepository.findById(id).orElse(null);

        if (property == null) {
            return null;
        }

        if (propertyDto.getTitle() != null) {
            property.setTitle(propertyDto.getTitle());
        }

        if (propertyDto.getPrice() != null) {
            property.setPrice(propertyDto.getPrice());
        }

        if (propertyDto.getLocation() != null) {
            property.setLocation(propertyDto.getLocation());
        }

        if (propertyDto.getAddress() != null) {
            property.setAddress(propertyDto.getAddress());
        }

        if (propertyDto.getCity() != null) {
            property.setCity(propertyDto.getCity());
        }

        if (propertyDto.getState() != null) {
            property.setState(propertyDto.getState());
        }

        if (propertyDto.getPincode() != null) {
            property.setPincode(propertyDto.getPincode());
        }

        if (propertyDto.getDescription() != null) {
            property.setDescription(propertyDto.getDescription());
        }

        if (propertyDto.getPropertyType() != null) {
            property.setPropertyType(propertyDto.getPropertyType());
        }

        if (propertyDto.getPgType() != null) {
            property.setPgType(propertyDto.getPgType());
        }

        if (propertyDto.getBhkType() != null) {
            property.setBhkType(propertyDto.getBhkType());
        }

        if (propertyDto.getFurnishing() != null) {
            property.setFurnishing(propertyDto.getFurnishing());
        }

        if (propertyDto.getCarpetArea() != null) {
            property.setCarpetArea(propertyDto.getCarpetArea());
        }

        if (propertyDto.getMobileNumber() != null) {
            property.setMobileNumber(propertyDto.getMobileNumber());
        }

        if (propertyDto.getApartmentName() != null) {
            property.setApartmentName(propertyDto.getApartmentName());
        }

        Property updatedProperty = propertyRepository.save(property);

        PropertyDto responseDto = new PropertyDto();

        fillBasicPropertyDto(responseDto, updatedProperty);
        attachDatabaseImages(responseDto, updatedProperty.getId());

        return responseDto;
    }

    @Override
    public String deleteProperty(Long id) {
        Property property =
                propertyRepository.findById(id).orElse(null);

        if (property == null) {
            return MessageConfig.PROPERTY_NOT_FOUND;
        }

        property.setStatus(AppConstants.INACTIVE);
        propertyRepository.save(property);

        return AppConstants.PROPERTY_DELETED;
    }

    @Override
    public String uploadPropertyImages(Long propertyId, MultipartFile[] files) {
        Property property =
                propertyRepository.findById(propertyId).orElse(null);

        if (property == null) {
            return MessageConfig.PROPERTY_NOT_FOUND;
        }

        int index = 0;
        StringBuilder doctypeImages = new StringBuilder();

        for (MultipartFile file : files) {
            try {
                String originalName = file.getOriginalFilename();
                String extension = "";

                if (originalName != null && originalName.contains(".")) {
                    extension =
                            originalName.substring(originalName.lastIndexOf("."));
                }

                String fileName =
                        System.currentTimeMillis() + "_" + index + extension;

                Long originalKb = file.getSize() / 1024;
                Double originalMb = file.getSize() / (1024.0 * 1024.0);

                PropertyImage image = new PropertyImage();

                image.setImageName(fileName);
                image.setImagePath(fileName);
                image.setContentType(file.getContentType());
                image.setImageData(file.getBytes());
                image.setOriginalSizeKb(originalKb);
                image.setOriginalSizeMb(originalMb);
                image.setCompressedSizeKb(originalKb);
                image.setCompressedSizeMb(originalMb);
                image.setProperty(property);

                propertyImageRepository.save(image);

                if (index == 0) {
                    property.setCoverImage(fileName);
                } else {
                    if (doctypeImages.length() > 0) {
                        doctypeImages.append(",");
                    }

                    doctypeImages.append(fileName);
                }

                index++;

            } catch (Exception e) {
                e.printStackTrace();
                return MessageConfig.IMAGE_UPLOAD_FAILED;
            }
        }

        if (doctypeImages.length() > 0) {
            property.setDoctypeImages(doctypeImages.toString());
        }

        int totalImages =
                propertyImageRepository.countByPropertyId(propertyId);


        if (property.getPremiumStatus() == PremiumStatus.FREE_ACTIVE
                || property.getPremiumStatus() == PremiumStatus.ACTIVE) {

            property.setStatus(AppConstants.ACTIVE);

        } else {
            property.setStatus(AppConstants.PENDING);
        }

        propertyRepository.save(property);

        if (totalImages < 4) {
            return AppConstants.UPLOAD_SUCCESSFULLY
                    + (4 - totalImages)
                    + AppConstants.MORE_IMG;
        }

        return MessageConfig.IMAGE_UPLOAD_SUCCESS;
    }

    @Override
    public List<?> filterProperties(PropertyFilterDto filterDto, Long userId) {
        User user =
                userRepository.findById(userId).orElse(null);

        boolean isPremium = false;

        if (user != null && user.isPremiumActive()) {
            isPremium = true;
        }

        List<Property> allProperties =
                propertyRepository.findByStatus(AppConstants.ACTIVE);

        List<Property> filteredList = new ArrayList<>();

        for (Property property : allProperties) {
            if (property.getPremiumStatus() != PremiumStatus.FREE_ACTIVE
                    && property.getPremiumStatus() != PremiumStatus.ACTIVE) {
                continue;
            }

            PropertyOwner owner = property.getPropertyOwner();

            if (owner == null) {
                continue;
            }

            boolean match = true;

            if (filterDto.getCity() != null
                    && !filterDto.getCity().isEmpty()) {

                if (property.getCity() == null
                        || !property.getCity()
                        .equalsIgnoreCase(filterDto.getCity())) {

                    match = false;
                }
            }

            if (filterDto.getAddress() != null
                    && !filterDto.getAddress().isBlank()) {

                if (!matchesLocationOrAddress(property, filterDto.getAddress())) {
                    match = false;
                }
            }

            if (filterDto.getPropertyType() != null
                    && !filterDto.getPropertyType().isEmpty()
                    && !filterDto.getPropertyType().equalsIgnoreCase("ALL")) {

                if (property.getPropertyType() == null
                        || !property.getPropertyType().name()
                        .equalsIgnoreCase(filterDto.getPropertyType())) {

                    match = false;
                }
            }

            if (filterDto.getPgType() != null
                    && !filterDto.getPgType().isEmpty()
                    && !filterDto.getPgType().equalsIgnoreCase("ALL")) {

                if (property.getPgType() == null
                        || !property.getPgType().name()
                        .equalsIgnoreCase(filterDto.getPgType())) {

                    match = false;
                }
            }

            if (filterDto.getMinPrice() != null
                    && property.getPrice() < filterDto.getMinPrice()) {

                match = false;
            }

            if (filterDto.getMaxPrice() != null
                    && property.getPrice() > filterDto.getMaxPrice()) {

                match = false;
            }

            if (match) {
                filteredList.add(property);
            }
        }

        List<PropertyDto> dtoList = new ArrayList<>();

        for (Property property : filteredList) {
            PropertyDto dto = new PropertyDto();
            attachDatabaseImages(dto, property.getId());

            if (!isPremium) {
                dto.setId(property.getId());
                dto.setTitle(property.getTitle());
                dto.setPrice(property.getPrice());
                dto.setLocation(property.getLocation());
                dto.setAddress(property.getAddress());
                dto.setCity(property.getCity());
                dto.setBhkType(property.getBhkType());
                dto.setPropertyType(property.getPropertyType());
                dto.setApartmentName(property.getApartmentName());
                dto.setStatus(property.getStatus());
                dto.setRented(property.getRented());
            } else {
                fillBasicPropertyDto(dto, property);
            }

            if (property.getPropertyOwner() != null) {
                dto.setOwnerId(property.getPropertyOwner().getOwnerId());
            }

            setPropertyPremiumFields(dto, property);

            dtoList.add(dto);
        }

        return dtoList;
    }

    public List<PropertyDto> getPropertiesByCityAndAddress(
            String city,
            String address
    ) {
        List<Property> list;

        if (address == null || address.isEmpty()) {
            list =
                    propertyRepository
                            .findByCityIgnoreCaseAndStatus(
                                    city,
                                    AppConstants.ACTIVE
                            );
        } else {
            list =
                    propertyRepository
                            .findByCityIgnoreCaseAndAddressIgnoreCaseAndStatus(
                                    city,
                                    address,
                                    AppConstants.ACTIVE
                            );
        }

        List<PropertyDto> dtoList = new ArrayList<>();

        for (Property property : list) {
            if (property.getPremiumStatus() != PremiumStatus.FREE_ACTIVE
                    && property.getPremiumStatus() != PremiumStatus.ACTIVE) {
                continue;
            }

            PropertyDto dto = new PropertyDto();

            dto.setId(property.getId());
            dto.setTitle(property.getTitle());
            dto.setPrice(property.getPrice());
            dto.setAddress(property.getAddress());
            dto.setCity(property.getCity());
            dto.setMobileNumber(property.getMobileNumber());
            dto.setBhkType(property.getBhkType());
            dto.setLocation(property.getLocation());
            dto.setApartmentName(property.getApartmentName());
            dto.setStatus(property.getStatus());
            dto.setRented(property.getRented());

            setPropertyPremiumFields(dto, property);
            attachDatabaseImages(dto, property.getId());

            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    public List<PropertyDto> getPropertiesByOwnerId(Long ownerId) {
        List<Property> properties =
                propertyRepository
                        .findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(ownerId);

        List<PropertyDto> dtoList = new ArrayList<>();

        for (Property property : properties) {
            PropertyDto dto = new PropertyDto();

            fillBasicPropertyDto(dto, property);


            PropertyOwner owner = property.getPropertyOwner();

            if (owner != null) {
                dto.setOwnerId(owner.getOwnerId());
                dto.setPremiumCount(owner.getPremiumCount());
            }

            attachDatabaseImages(dto, property.getId());

            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    public String activateProperty(Long id) {
        Property property =
                propertyRepository.findById(id).orElse(null);

        if (property == null) {
            return MessageConfig.PROPERTY_NOT_FOUND;
        }

        property.setStatus(AppConstants.ACTIVE);

        if (property.getPremiumStatus() == null
                || property.getPremiumStatus() == PremiumStatus.NONE
                || property.getPremiumStatus() == PremiumStatus.PENDING_APPROVAL) {

            property.setPremiumStatus(PremiumStatus.ACTIVE);
            property.setPremiumActive(true);
        }

        propertyRepository.save(property);

        return "Property Activated Successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PropertyImageContent> getPropertyImageContent(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }

        String decodedFilename =
                URLDecoder.decode(filename.trim(), StandardCharsets.UTF_8);

        if (decodedFilename.contains("..")
                || decodedFilename.contains("/")
                || decodedFilename.contains("\\")) {

            return Optional.empty();
        }

        try {
            Optional<PropertyImage> imageOptional =
                    propertyImageRepository.findFirstByImagePath(decodedFilename);

            if (imageOptional.isPresent()) {
                PropertyImage image = imageOptional.get();

                byte[] data = image.getImageData();

                if (data != null && data.length > 0) {
                    return Optional.of(
                            new PropertyImageContent(
                                    data,
                                    resolveContentType(
                                            image.getContentType(),
                                            decodedFilename
                                    )
                            )
                    );
                }
            }
        } catch (Exception ignored) {
        }

        return readLegacyDiskImage(decodedFilename);
    }

    private Optional<PropertyImageContent> readLegacyDiskImage(String filename) {
        try {
            Path imagePath =
                    Path.of(propertyImagesDir)
                            .resolve(filename)
                            .normalize();

            Path baseDir =
                    Path.of(propertyImagesDir)
                            .toAbsolutePath()
                            .normalize();

            if (!imagePath.toAbsolutePath()
                    .normalize()
                    .startsWith(baseDir)) {

                return Optional.empty();
            }

            if (!Files.isRegularFile(imagePath)) {
                return Optional.empty();
            }

            byte[] data = Files.readAllBytes(imagePath);

            if (data.length == 0) {
                return Optional.empty();
            }

            return Optional.of(
                    new PropertyImageContent(
                            data,
                            resolveContentType(null, filename)
                    )
            );

        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String resolveContentType(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }

        String lower = filename.toLowerCase();

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        if (lower.endsWith(".webp")) {
            return "image/webp";
        }

        if (lower.endsWith(".gif")) {
            return "image/gif";
        }

        return "image/jpeg";
    }

    @Override
    public String toggleLikeProperty(Long propertyId, Long userId) {
        Property property =
                propertyRepository.findById(propertyId).orElse(null);

        if (property == null) {
            return "Property Not Found";
        }

        User user =
                userRepository.findById(userId).orElse(null);

        if (user == null) {
            return "User Not Found";
        }

        PropertyLike existingLike =
                propertyLikeRepository
                        .findByUser_UserIdAndProperty_Id(userId, propertyId)
                        .orElse(null);

        if (existingLike != null) {
            propertyLikeRepository.delete(existingLike);

            Integer count =
                    propertyLikeRepository.countByProperty_Id(propertyId);

            property.setLikesCount(count);
            propertyRepository.save(property);

            return "Property Unliked Successfully";
        }

        PropertyLike like = new PropertyLike();

        like.setUser(user);
        like.setProperty(property);

        propertyLikeRepository.save(like);

        Integer count =
                propertyLikeRepository.countByProperty_Id(propertyId);

        property.setLikesCount(count);
        propertyRepository.save(property);

        return "Property Liked Successfully";
    }

    @Override
    public List<PropertyDto> getLikedProperties(Long userId) {
        List<PropertyLike> likes =
                propertyLikeRepository.findByUser_UserId(userId);

        List<PropertyDto> dtoList = new ArrayList<>();

        for (PropertyLike like : likes) {
            Property property = like.getProperty();

            if (property == null) {
                continue;
            }

            if (property.getPremiumStatus() != PremiumStatus.FREE_ACTIVE
                    && property.getPremiumStatus() != PremiumStatus.ACTIVE) {
                continue;
            }

            PropertyDto dto = new PropertyDto();

            dto.setId(property.getId());
            dto.setTitle(property.getTitle());
            dto.setPrice(property.getPrice());
            dto.setLocation(property.getLocation());
            dto.setAddress(property.getAddress());
            dto.setCity(property.getCity());
            dto.setLikesCount(property.getLikesCount());
            dto.setLiked(true);
            dto.setStatus(property.getStatus());
            dto.setRented(property.getRented());

            setPropertyPremiumFields(dto, property);
            attachDatabaseImages(dto, property.getId());

            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    public Integer getUserLikedPropertiesCount(Long userId) {
        return propertyLikeRepository
                .findByUser_UserId(userId)
                .size();
    }

    @Override
    public String markPropertyAsRented(Long propertyId) {

        Property property = propertyRepository.findById(propertyId).orElse(null);

        if (property == null) {
            return MessageConfig.PROPERTY_NOT_FOUND;
        }

        property.setRented(true);

        propertyRepository.save(property);

        return "Property Marked As RENTED Successfully";
    }
}
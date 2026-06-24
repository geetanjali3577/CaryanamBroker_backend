//package com.caryanam.caryanam_broker.dto;
//
//
//import com.caryanam.caryanam_broker.Enum.BhkType;
//import com.caryanam.caryanam_broker.Enum.FurnishingType;
//import com.caryanam.caryanam_broker.Enum.PgType;
//import com.caryanam.caryanam_broker.Enum.PropertyType;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import lombok.Data;
//
//import java.util.List;
//
//
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@Data
//public class PropertyDto {
//
//    private Long id;
//    private String title;
//
//    private Double  price;
//    private String location;
//    private String description;
//    private PropertyType propertyType;
//    private PgType pgType;
//    private String mobileNumber;
//    private Integer likesCount;
//    private Integer viewsCount;
//    private String status;
//
//    private String city;
//    private String address;
//    private String state;
//    private String pincode;
//
//    private BhkType bhkType;
//    private FurnishingType furnishing;
//    private String carpetArea;
//    private String coverImage;
//    private String coverImageBase64;
//    private String doctypeImages;
//    private List<String> doctypeImageBase64List;
//    private Long ownerId;
//    private String apartmentName;
//    private String ownerName;
//    private boolean premiumActive;
//    private String paymentStatus;
//    private String nearBy;
//
//    private String premiumStatus;
//    private String coverImageUrl;
//
//    private List<String> imageUrls;
//    private Integer premiumCount;
//    private Integer liked;
//
//
//    public void setLiked(boolean b) {
//    }
//}

package com.caryanam.caryanam_broker.dto;

import com.caryanam.caryanam_broker.Enum.BhkType;
import com.caryanam.caryanam_broker.Enum.FurnishingType;
import com.caryanam.caryanam_broker.Enum.PgType;
import com.caryanam.caryanam_broker.Enum.PropertyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PropertyDto {

    private Long id;
    private String title;

    private Double price;
    private String location;
    private String description;
    private PropertyType propertyType;
    private PgType pgType;
    private String mobileNumber;
    private Integer likesCount;
    private Integer viewsCount;
    private String status;

    private String city;
    private String address;
    private String state;
    private String pincode;

    private BhkType bhkType;
    private FurnishingType furnishing;
    private String carpetArea;

    private String coverImage;
    private String coverImageBase64;
    private String coverImageUrl;

    private String doctypeImages;
    private List<String> doctypeImageBase64List;
    private List<String> imageUrls;

    private Long ownerId;
    private String ownerName;
    private String apartmentName;

    private String nearBy;

    // ================= PREMIUM / PAYMENT FIELDS =================

    private Boolean premiumActive;
    private String paymentStatus;
    private String premiumStatus;
    private Boolean isFirstFreeProperty;
    private Integer premiumCount;

    // ================= LIKE FIELD =================

    private Boolean liked;

    /*
      Explicit getter/setter ठेवले आहेत कारण काही वेळा Lombok
      Boolean field "isFirstFreeProperty" साठी method name mismatch करतो.
    */
    public Boolean getIsFirstFreeProperty() {
        return isFirstFreeProperty;
    }

    public void setIsFirstFreeProperty(Boolean isFirstFreeProperty) {
        this.isFirstFreeProperty = isFirstFreeProperty;
    }
}
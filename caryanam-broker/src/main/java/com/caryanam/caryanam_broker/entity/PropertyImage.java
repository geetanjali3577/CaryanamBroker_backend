////package com.caryanam.caryanam_broker.entity;
////
////
////import jakarta.persistence.*;
////import lombok.Data;
////
////@Data
////@Entity
////@Table(name = "property_images")
////public class PropertyImage {
////
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    private Long id;
////    private String imageName;
////    private String imagePath;
////    @ManyToOne
////    @JoinColumn(name = "property_id")
////    private Property property;
////
////    private Double originalSizeMb;
////    private Long originalSizeKb;
////    private Double compressedSizeMb;
////    private Long compressedSizeKb;
////}
//
//package com.caryanam.caryanam_broker.entity;
//
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Data
//@Entity
//@Table(name = "property_images")
//public class PropertyImage {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    private String imageName;
//    private String imagePath;
//    private String contentType;
//
//    @Lob
//    @Column(columnDefinition = "LONGBLOB")
//    private byte[] imageData;
//
//    @ManyToOne
//    @JoinColumn(name = "property_id")
//    private Property property;
//
//    private Double originalSizeMb;
//    private Long originalSizeKb;
//    private Double compressedSizeMb;
//    private Long compressedSizeKb;
//}

package com.caryanam.caryanam_broker.entity;

import jakarta.persistence.*;

@Entity
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageName;

    private String imagePath;

    private String contentType;

    private Long originalSizeKb;

    private Double originalSizeMb;

    private Long compressedSizeKb;

    private Double compressedSizeMb;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @ManyToOne
    @JoinColumn(name = "property_id")
    private Property property;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getOriginalSizeKb() {
        return originalSizeKb;
    }

    public void setOriginalSizeKb(Long originalSizeKb) {
        this.originalSizeKb = originalSizeKb;
    }

    public Double getOriginalSizeMb() {
        return originalSizeMb;
    }

    public void setOriginalSizeMb(Double originalSizeMb) {
        this.originalSizeMb = originalSizeMb;
    }

    public Long getCompressedSizeKb() {
        return compressedSizeKb;
    }

    public void setCompressedSizeKb(Long compressedSizeKb) {
        this.compressedSizeKb = compressedSizeKb;
    }

    public Double getCompressedSizeMb() {
        return compressedSizeMb;
    }

    public void setCompressedSizeMb(Double compressedSizeMb) {
        this.compressedSizeMb = compressedSizeMb;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }
}
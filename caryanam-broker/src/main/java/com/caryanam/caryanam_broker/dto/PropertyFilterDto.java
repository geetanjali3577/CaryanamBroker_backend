package com.caryanam.caryanam_broker.dto;


import lombok.Data;

@Data
public class PropertyFilterDto {

    private Double minPrice;
    private Double maxPrice;
    private String propertyType;
    private String sortBy;
    private String city;
    private String address;
    private Boolean fetchAddressOnly;
    private String pgType;

}
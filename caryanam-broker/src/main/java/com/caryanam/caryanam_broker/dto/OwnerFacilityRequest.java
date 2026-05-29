package com.caryanam.caryanam_broker.dto;



import java.util.List;

import lombok.Data;


@Data
public class OwnerFacilityRequest {
    private Long ownerId;
    private Long propertyId;

    private List<FacilityDto> facilities;
}

package com.caryanam.caryanam_broker.dto;

import com.caryanam.caryanam_broker.enums.FacilityName;
import com.caryanam.caryanam_broker.enums.FacilityStatus;
import lombok.Data;

@Data
public class FacilityDto {

    private FacilityName facilityName;

    private FacilityStatus status;
}


package com.caryanam.caryanam_broker.configuration;

import lombok.Getter;
import lombok.Setter;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class PhonePeConfig {

    @Value("${phonepe.client-id}")
    private String clientId;

    @Value("${phonepe.client-secret}")
    private String clientSecret;

    @Value("${phonepe.client-version}")
    private Integer clientVersion;
}

//package com.caryanam.caryanam_broker.configuration;
//
//import lombok.Getter;
//import lombok.Setter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@Getter
//@Setter
//public class PhonePeConfig {
//
//    @Value("${phonepe.merchant-id}")
//    private String merchantId;
//
//    @Value("${phonepe.salt-key}")
//    private String saltKey;
//
//    @Value("${phonepe.salt-index}")
//    private String saltIndex;
//
//    @Value("${phonepe.pay-url}")
//    private String payUrl;
//
//    @Value("${phonepe.status-url}")
//    private String statusUrl;
//
//    @Value("${phonepe.redirect-url}")
//    private String redirectUrl;
//
//    @Value("${phonepe.callback-url}")
//    private String callbackUrl;
//
//    @Value("${phonepe.webhook.username}")
//    private String webhookUsername;
//
//    @Value("${phonepe.webhook.password}")
//    private String webhookPassword;
//}

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
    private String clientVersion;

    @Value("${phonepe.auth-url}")
    private String authUrl;

    @Value("${phonepe.pay-url}")
    private String payUrl;

    @Value("${phonepe.redirect-url}")
    private String redirectUrl;

    @Value("${phonepe.webhook.username}")
    private String webhookUsername;

    @Value("${phonepe.webhook.password}")
    private String webhookPassword;

    @Value("${phonepe.sdk-order-url}")
    private String sdkOrderUrl;

    @Value("${phonepe.status-url}")
    private String statusUrl;

    @Value("${phonepe.environment}")
    private String environment;

    @Value("${phonepe.merchant-id}")
    private String merchantId;
}
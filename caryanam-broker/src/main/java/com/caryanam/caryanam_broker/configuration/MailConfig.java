package com.caryanam.caryanam_broker.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost("smtp.hostinger.com");
        mailSender.setPort(465);

        mailSender.setUsername("support@rentalchaavi.com");

        // Password madhye extra space nako
        mailSender.setPassword("Kharadi@6006");

        Properties props = mailSender.getJavaMailProperties();

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        // Port 465 sathi SSL enable
        props.put("mail.smtp.ssl.enable", "true");

        // STARTTLS disable
        props.put("mail.smtp.starttls.enable", "false");

        props.put("mail.smtp.ssl.trust", "smtp.hostinger.com");

        props.put("mail.debug", "true");

        return mailSender;
    }
}
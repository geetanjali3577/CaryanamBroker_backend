package com.caryanam.caryanam_broker.serviceimpl;

import com.caryanam.caryanam_broker.entity.Admin;
import com.caryanam.caryanam_broker.enums.Role;
import com.caryanam.caryanam_broker.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void createDefaultAdmin() {

        String defaultEmail = "admin@gmail.com";

        boolean exists = adminRepository.existsByEmail(defaultEmail);

        if (!exists) {
            Admin admin = new Admin();
            admin.setFullName("Super Admin");
            admin.setEmail(defaultEmail);
            admin.setMobileNumber("9812348171");
            admin.setPassword(passwordEncoder.encode("admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setIsActive("true");

            adminRepository.save(admin);

        }
    }
}
package com.caryanam.caryanam_broker.configuration;

import com.caryanam.caryanam_broker.entity.Admin;
import com.caryanam.caryanam_broker.entity.PropertyOwner;
import com.caryanam.caryanam_broker.entity.User;
import com.caryanam.caryanam_broker.repository.AdminRepository;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;


//    @Override
//    public UserDetails loadUserByUsername(String email)
//            throws UsernameNotFoundException {
//
//        //  Check USER
//        User user = userRepository.findByEmail(email).orElse(null);
//        if (user != null) {
//
//            String role = user.getRole() != null ? user.getRole().name() : "USER";
//
//            return new org.springframework.security.core.userdetails.User(
//                    user.getEmail(),
//                    user.getPassword(),
//                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
//            );
//        }
//
//        //  Check ADMIN
//        Admin admin = adminRepository.findByEmail(email).orElse(null);
//        if (admin != null) {
//
//            String role = admin.getRole() != null ? admin.getRole().name() : "ADMIN";
//
//            return new org.springframework.security.core.userdetails.User(
//                    admin.getEmail(),
//                    admin.getPassword(),
//                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
//            );
//        }
//
//        //  Check PROPERTY OWNER
//        PropertyOwner owner = propertyOwnerRepository.findByEmail(email).orElse(null);
//        if (owner != null) {
//
//            String role = owner.getRole() != null ? owner.getRole().name() : "PROPERTY_OWNER";
//
//            return new org.springframework.security.core.userdetails.User(
//                    owner.getEmail(),
//                    owner.getPassword(),
//                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
//            );
//        }
//
//
//        throw new UsernameNotFoundException("User not found with email: " + email);
//    }}

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // USER
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {

            String role = user.getRole() != null ? user.getRole().name() : "USER";

            return new CustomUserDetails(
                    user.getUserId(),
                    user.getEmail(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }

        // ADMIN
        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin != null) {

            String role = admin.getRole() != null ? admin.getRole().name() : "ADMIN";

            return new CustomUserDetails(
                    admin.getAdminId(),
                    admin.getEmail(),
                    admin.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }

        // OWNER
        PropertyOwner owner = propertyOwnerRepository.findByEmail(email).orElse(null);
        if (owner != null) {

            String role = owner.getRole() != null ? owner.getRole().name() : "PROPERTY_OWNER";

            return new CustomUserDetails(
                    owner.getOwnerId(),
                    owner.getEmail(),
                    owner.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
package com.caryanam.caryanam_broker.configuration;

import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SocketConfig {

    private final UserRepository userRepo;
    private final PropertyOwnerRepository ownerRepo;

    @Bean
    public SocketIOServer socketIOServer() {

        com.corundumstudio.socketio.Configuration config =
                new com.corundumstudio.socketio.Configuration();

        //  BASIC CONFIG
         config.setHostname("localhost");
                config.setHostname("0.0.0.0");
        config.setPort(9092);


        config.setOrigin("http://localhost:63342");
        config.setOrigin(null);

        config.setPingInterval(25000);
        config.setPingTimeout(60000);


        config.setAuthorizationListener(data -> {

            String userIdStr = data.getSingleUrlParam("userId");
            String ownerIdStr = data.getSingleUrlParam("ownerId");


            if (userIdStr != null && ownerIdStr != null) {
                System.out.println(" BOTH userId & ownerId present");
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }


            if (userIdStr == null && ownerIdStr == null) {
                System.out.println(" No ID provided");
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }


            if (userIdStr != null) {
                try {
                    Long userId = Long.valueOf(userIdStr);

                    if (userId <= 0) {
                        System.out.println(" Invalid USER ID (<=0)");
                        return AuthorizationResult.FAILED_AUTHORIZATION;
                    }

                    if (userRepo.existsById(userId)) {
                        System.out.println(" Connected USER: " + userId);
                        return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
                    }

                    System.out.println(" USER not found: " + userId);

                } catch (Exception e) {
                    System.out.println(" Invalid USER ID format");
                }
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }


            if (ownerIdStr != null) {
                try {
                    Long ownerId = Long.valueOf(ownerIdStr);

                    if (ownerId <= 0) {
                        System.out.println(" Invalid OWNER ID (<=0)");
                        return AuthorizationResult.FAILED_AUTHORIZATION;
                    }

                    if (ownerRepo.existsById(ownerId)) {
                        System.out.println(" Connected OWNER: " + ownerId);
                        return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
                    }

                    System.out.println(" OWNER not found: " + ownerId);

                } catch (Exception e) {
                    System.out.println(" Invalid OWNER ID format");
                }
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            return AuthorizationResult.FAILED_AUTHORIZATION;
        });

        return new SocketIOServer(config);
    }
}

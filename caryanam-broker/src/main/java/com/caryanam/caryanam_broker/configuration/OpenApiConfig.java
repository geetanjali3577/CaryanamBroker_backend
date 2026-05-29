package com.caryanam.caryanam_broker.configuration;


import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            Server localServer = new Server();
            localServer.setUrl("http://localhost:8080/swagger-ui.html");
            localServer.setDescription("Local Server");

            Server productionServer = new Server();
            productionServer.setUrl("https://cffffftasting-production.up.railway.app/");
            productionServer.setDescription("Production Server");

            openApi.setServers(List.of(localServer, productionServer));
        };
    }
}
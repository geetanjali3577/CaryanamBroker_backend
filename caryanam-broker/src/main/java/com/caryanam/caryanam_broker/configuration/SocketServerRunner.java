package com.caryanam.caryanam_broker.configuration;


import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class SocketServerRunner {

    private final SocketIOServer server;

    public SocketServerRunner(SocketIOServer server) {
        this.server = server;
    }

    @PostConstruct
    public void start() {
        server.start();
        System.out.println(" Socket.IO Server Started on port 9092");
    }

    @PreDestroy
    public void stop() {
        server.stop();
        System.out.println(" Socket.IO Server Stopped");
    }
}

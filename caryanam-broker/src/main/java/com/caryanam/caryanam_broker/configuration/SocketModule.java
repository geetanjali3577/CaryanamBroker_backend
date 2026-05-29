package com.caryanam.caryanam_broker.configuration;
import com.caryanam.caryanam_broker.service.ChatService;
import com.caryanam.caryanam_broker.socket.MessageRequestDTO;
import com.caryanam.caryanam_broker.socket.MessageResponseDTO;
import com.caryanam.caryanam_broker.socket.TypingDTO;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class SocketModule {

    private final SocketIOServer server;
    private final ChatService chatService;

    public SocketModule(SocketIOServer server, ChatService chatService) {
        this.server = server;
        this.chatService = chatService;
    }

    @PostConstruct
    public void init() {


        server.addConnectListener(client -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            String ownerId = client.getHandshakeData().getSingleUrlParam("ownerId");

            if (userId != null) {
                System.out.println("USER Connected: " + userId);
            } else if (ownerId != null) {
                System.out.println("PROPERTY OWNER Connected: " + ownerId);
            }

            System.out.println("Session ID: " + client.getSessionId());
        });


        server.addDisconnectListener(client -> {
            System.out.println("Client Disconnected: " + client.getSessionId());
        });


        server.addEventListener("join_room", String.class, (client, roomId, ackSender) -> {

            client.joinRoom(roomId);

            System.out.println("Joined Room: " + roomId);

            int count = server.getRoomOperations(roomId).getClients().size();
            System.out.println("TOTAL CLIENTS IN ROOM: " + count);


            try {
                client.sendEvent("chat_history", chatService.getMessagesByRoom(roomId));
            } catch (Exception e) {
                System.out.println("Error sending history: " + e.getMessage());
            }
        });


        server.addEventListener("send_message", MessageRequestDTO.class, (client, dto, ackSender) -> {

            System.out.println("Message received: " + dto);

            MessageResponseDTO response = chatService.sendMessage(dto);

            String roomId = response.getRoomId();

            System.out.println("Broadcasting to ROOM: " + roomId);


        });


        server.addEventListener("typing", TypingDTO.class, (client, dto, ackSender) -> {

            if (dto.getRoomId() == null) return;

            for (SocketIOClient c : server.getRoomOperations(dto.getRoomId()).getClients()) {
                if (!c.getSessionId().equals(client.getSessionId())) {
                    c.sendEvent("typing", dto);
                }
            }

            chatService.handleTyping(dto);
        });
    }
}
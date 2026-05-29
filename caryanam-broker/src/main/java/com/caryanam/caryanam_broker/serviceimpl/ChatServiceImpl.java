package com.caryanam.caryanam_broker.serviceimpl;


import com.caryanam.caryanam_broker.entity.*;
import com.caryanam.caryanam_broker.enums.MessageStatus;
import com.caryanam.caryanam_broker.exception.BadRequestException;
import com.caryanam.caryanam_broker.repository.*;
import com.caryanam.caryanam_broker.service.ChatService;
import com.caryanam.caryanam_broker.socket.*;

import com.corundumstudio.socketio.SocketIOClient;
import jdk.jshell.Snippet;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOServer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatRoomRepository chatRoomRepo;
    @Autowired
    private MessageRepository messageRepo;
    @Autowired
    private SocketIOServer socketServer;
    @Autowired
    private UserStatusRepository statusRepo;


    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String generateRoomId(Long userId, Long ownerId) {
        return "USER_" + userId + "_OWNER_" + ownerId;
    }

    @Override
    public String createOrGetRoom(Long userId, Long ownerId) {

    String roomId = generateRoomId(userId, ownerId);

    return chatRoomRepo.findByUserIdAndOwnerId(userId, ownerId)
            .map(ChatRoom::getRoomId)
            .orElseGet(() -> {
                ChatRoom room = new ChatRoom();
                room.setUserId(userId);
                room.setOwnerId(ownerId);
                room.setRoomId(roomId);
                room.setFirstMessageSent(false);

                chatRoomRepo.save(room);
                return roomId;
            });
}

@Override
public MessageResponseDTO sendMessage(MessageRequestDTO dto) {


    if (dto == null || dto.getUserId() == null || dto.getOwnerId() == null) {
        throw new BadRequestException("Invalid request");
    }

    if (!"USER".equals(dto.getSenderRole()) && !"PROPERTY_OWNER".equals(dto.getSenderRole())) {
        throw new BadRequestException("Invalid sender role");
    }

    Long userId = dto.getUserId();
    Long ownerId = dto.getOwnerId();

    String roomId = createOrGetRoom(userId, ownerId);

    ChatRoom room = chatRoomRepo.findByRoomId(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

    if (!room.isFirstMessageSent()) {

        if (!"USER".equals(dto.getSenderRole())) {
            throw new BadRequestException("First message must be sent by USER");
        }

        Message firstMsg = new Message();
        firstMsg.setRoomId(roomId);
        firstMsg.setSenderId(userId);
        firstMsg.setSenderRole("USER");


        String message = (dto.getMessage() == null || dto.getMessage().trim().split("\\s+").length < 10)
                ? "Hi, I’m interested in your property. Could you please share more information?"
                : dto.getMessage();

        firstMsg.setContent(message);
        firstMsg.setTimestamp(LocalDateTime.now());
        firstMsg.setRead(false);
        firstMsg.setStatus(MessageStatus.PENDING);

        messageRepo.save(firstMsg);

        room.setFirstMessageSent(true);
        chatRoomRepo.save(room);

        MessageResponseDTO response = mapToDTO(firstMsg);
        socketServer.getRoomOperations(roomId).sendEvent("receive_message", response);

        return response;
    }


    //  AFTER FIRST MESSAGE → EMPTY MESSAGE BLOCK

     //    if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
       //        throw new BadRequestException("Message cannot be empty");
        //    }


    if (!room.isAccepted() && "USER".equals(dto.getSenderRole())) {
        throw new BadRequestException("Please wait until owner accepts the chat");
    }

    if (!room.isAccepted() && "PROPERTY_OWNER".equals(dto.getSenderRole())) {
        throw new BadRequestException("Owner cannot send message until chat is accepted");
    }

    if (room.isRejected()) {
        throw new BadRequestException("Chat is rejected. You cannot send messages.");
    }

    Message msg = new Message();
    msg.setRoomId(roomId);

    Long senderId = "USER".equals(dto.getSenderRole())
            ? userId
            : ownerId;

    msg.setSenderId(senderId);
    msg.setSenderRole(dto.getSenderRole());
    msg.setContent(dto.getMessage());
    msg.setTimestamp(LocalDateTime.now());
    msg.setRead(false);
    msg.setStatus(room.isAccepted() ? MessageStatus.ACCEPTED : MessageStatus.PENDING);

    messageRepo.save(msg);

    MessageResponseDTO response = mapToDTO(msg);
    socketServer.getRoomOperations(roomId).sendEvent("receive_message", response);

    return response;
}

    @Override
    public void acceptChat(String roomId) {
        ChatRoom room = chatRoomRepo.findByRoomId(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
        if (room.isAccepted()) {
            throw new BadRequestException("Chat is already accepted");
        }
        if (room.isRejected()) {
            throw new BadRequestException("Rejected chat cannot be accepted");
        }
        if (!room.isFirstMessageSent()) {
            throw new BadRequestException("No chat request to accept");
        }
        room.setAccepted(true);
        chatRoomRepo.save(room);
        List<Message> messages = messageRepo.findByRoomId(roomId);
        for (Message msg : messages) {
            if ("USER".equals(msg.getSenderRole())) {
                msg.setStatus(MessageStatus.ACCEPTED);
                messageRepo.save(msg);
            }
        }
        Message autoReply = new Message();
        autoReply.setRoomId(roomId);
        autoReply.setSenderId(room.getOwnerId());
        autoReply.setSenderRole("PROPERTY_OWNER");
        autoReply.setContent("Please wait, someone will connect with you shortly.");
        autoReply.setTimestamp(LocalDateTime.now());
        autoReply.setRead(false);
        autoReply.setStatus(MessageStatus.ACCEPTED);
        messageRepo.save(autoReply);
        MessageResponseDTO response = mapToDTO(autoReply);
        socketServer.getRoomOperations(roomId).sendEvent("chat_accepted", response);
        socketServer.getRoomOperations(roomId).sendEvent("receive_message", response);
    }

    @Override
    public void rejectChat(String roomId) {

    ChatRoom room = chatRoomRepo.findByRoomId(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

    if (room.isRejected()) {throw new BadRequestException("Chat is already rejected");}
    if (room.isAccepted()) {throw new BadRequestException("Accepted chat cannot be rejected");}
    if (!room.isFirstMessageSent()) {throw new BadRequestException("No chat exists to reject");}

    room.setRejected(true);
    room.setAccepted(false);
    chatRoomRepo.save(room);
        List<Message> messages = messageRepo.findByRoomId(roomId);
        for (Message msg : messages) {
            if ("USER".equals(msg.getSenderRole())) {
                msg.setStatus(MessageStatus.REJECTED);
                messageRepo.save(msg);
            }
        }


    Message rejectMsg = new Message();
    rejectMsg.setRoomId(roomId);
    rejectMsg.setSenderId(room.getOwnerId());
    rejectMsg.setSenderRole("SYSTEM");
    rejectMsg.setContent("Chat has been rejected by the owner.");
    rejectMsg.setTimestamp(LocalDateTime.now());
    rejectMsg.setStatus(MessageStatus.REJECTED);

    messageRepo.save(rejectMsg);
    socketServer.getRoomOperations(roomId).sendEvent("chat_rejected", mapToDTO(rejectMsg));
}

    @Override
    public void handleTyping(TypingDTO dto) {
        if (dto.getRoomId() == null) {
            throw new BadRequestException("RoomId is required");
        }
        chatRoomRepo.findByRoomId(dto.getRoomId())
                .orElseThrow(() -> new RuntimeException("Invalid roomId"));

        socketServer.getRoomOperations(dto.getRoomId()).sendEvent("typing", dto);
    }

    @Override
    public void updateUserStatus(Long userId, boolean online) {
        if (userId == null) return;
        UserStatus status = statusRepo.findById(userId)
                .orElseGet(() -> {UserStatus s = new UserStatus();
                    s.setUserId(userId);
                    return s;});

        status.setOnline(online);
        statusRepo.save(status);
        socketServer.getBroadcastOperations().sendEvent("user_status", status);
    }

    @Override
    public MessageResponseDTO mapToDTO(Message msg) {

        String time = null;
        if (msg.getTimestamp() != null) {
            time = msg.getTimestamp().format(FORMATTER);}

        return new MessageResponseDTO(
                msg.getRoomId(),
                msg.getSenderId(),
                msg.getSenderRole(),
                msg.getContent(),
                time);
    }

    @Override
    public List<PendingChatDTO> getPendingChats(Long ownerId) {

        if (ownerId == null) {
            throw new RuntimeException("OwnerId is required");
        }

        List<ChatRoom> rooms = chatRoomRepo.findByOwnerIdAndFirstMessageSentTrueAndAcceptedFalseAndIsRejectedFalse(ownerId);

        List<PendingChatDTO> response = new ArrayList<>();
        for (ChatRoom room : rooms) {


            Message lastMsg = messageRepo.findTopByRoomIdOrderByTimestampDesc(room.getRoomId());

            String lastMessage = "";
            String time = "";
            if (lastMsg != null) {
                lastMessage = lastMsg.getContent();

                if (lastMsg.getTimestamp() != null) {
                    time = lastMsg.getTimestamp().toString();
                }
            }
            response.add(new PendingChatDTO(
                    room.getRoomId(),
                    room.getUserId(),
                    room.getOwnerId(),
                    lastMessage,
                    time));
        }
        return response;
    }
    @Override
    public List<AcceptedChatDTO> getAcceptedChats(Long ownerId) {

        List<ChatRoom> rooms = chatRoomRepo.findByOwnerIdAndAcceptedTrue(ownerId);
        List<AcceptedChatDTO> response = new ArrayList<>();

        for (ChatRoom room : rooms) {
            Message lastMsg = messageRepo.findTopByRoomIdOrderByTimestampDesc(room.getRoomId());
            String lastMessage = "";
            String time = "";

            if (lastMsg != null) {
                lastMessage = lastMsg.getContent();
                if (lastMsg.getTimestamp() != null) {
                    time = lastMsg.getTimestamp().toString();
                }
            }

            response.add(new AcceptedChatDTO(
                    room.getRoomId(),
                    room.getUserId(),
                    room.getOwnerId(),
                    lastMessage,
                    time));
        }

        return response;
    }

    @Override
    public List<PendingChatDTO> getRejectedChats(Long ownerId) {

        List<ChatRoom> rooms = chatRoomRepo.findByOwnerIdAndIsRejectedTrue(ownerId);
        List<PendingChatDTO> response = new ArrayList<>();

        for (ChatRoom room : rooms) {
            Message lastMsg = messageRepo.findTopByRoomIdOrderByTimestampDesc(room.getRoomId());

            String lastMessage = "";
            String time = "";
            if (lastMsg != null) {
                lastMessage = lastMsg.getContent();
                if (lastMsg.getTimestamp() != null) {
                    time = lastMsg.getTimestamp().toString();
                }
            }

            response.add(new PendingChatDTO(
                    room.getRoomId(),
                    room.getUserId(),
                    room.getOwnerId(),
                    lastMessage,
                    time));
        }
        return response;
    }

    @Override
    public List<MessageResponseDTO> getMessagesByRoom(String roomId) {

        return messageRepo.findByRoomId(roomId)
                .stream()
                .map(this::mapToDTO)   // your existing method
                .toList();
    }
}
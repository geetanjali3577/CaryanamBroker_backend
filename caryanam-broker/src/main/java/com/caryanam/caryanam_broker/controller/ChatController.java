package com.caryanam.caryanam_broker.controller;

import com.caryanam.caryanam_broker.dto.ResponseDto;
import com.caryanam.caryanam_broker.dto.ResponseHandler;
import com.caryanam.caryanam_broker.dto.UserStatusDTO;
import com.caryanam.caryanam_broker.enums.Role;
import com.caryanam.caryanam_broker.exception.BadRequestException;
import com.caryanam.caryanam_broker.exception.InvalidOperationException;
import com.caryanam.caryanam_broker.repository.AdminRepository;
import com.caryanam.caryanam_broker.repository.MessageRepository;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.UserRepository;
import com.caryanam.caryanam_broker.service.ChatService;
import com.caryanam.caryanam_broker.socket.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    @Autowired
    private  ChatService chatService;
    @Autowired
    private  UserRepository userRepository;
    @Autowired
    private PropertyOwnerRepository ownerRepository;

    @Autowired
    private MessageRepository messageRepo;



@PostMapping("/send")
public ResponseEntity<?> sendMessage(@RequestBody MessageRequestDTO dto) {

    if (dto == null) {
        throw new InvalidOperationException("Request body is missing");
    }
    if (dto.getUserId() == null || dto.getOwnerId() == null) {
        throw new InvalidOperationException("UserId and OwnerId are required");
    }
    if (!"USER".equals(dto.getSenderRole()) && !"PROPERTY_OWNER".equals(dto.getSenderRole())) {
        throw new InvalidOperationException("Invalid sender role");
    }

    Long userId = dto.getUserId();
    Long ownerId = dto.getOwnerId();

    if (!userRepository.existsById(userId)) {
        throw new InvalidOperationException("User not found with id: " + userId);
    }

    if (!ownerRepository.existsById(ownerId)) {
        throw new InvalidOperationException("Property Owner not found with id: " + ownerId);
    }

    MessageResponseDTO response = chatService.sendMessage(dto);

    return ResponseEntity.ok(
            new ResponseDto<>(200, "Message processed successfully", response));
}

//    @PostMapping("/room")
//    public ResponseEntity<ResponseDto<String>> createRoom(
//            @Valid @RequestBody RoomRequestDTO request) {
//
//
//        if (request.getUserId() == null || request.getOwnerId() == null) {throw new BadRequestException("UserId and OwnerId are required");}
//        if (!userRepository.existsById(request.getUserId())) {throw new BadRequestException("User not found: " + request.getUserId());}
//        if (!ownerRepository.existsById(request.getOwnerId())) {throw new BadRequestException("Owner not found: " + request.getOwnerId());}
//        String roomId = chatService.createOrGetRoom(
//                request.getUserId(),
//                request.getOwnerId());
//
//        return ResponseEntity.ok(new ResponseDto<>(200, "Room created/fetched successfully", roomId));
//    }


    @PostMapping("/accept")
    public ResponseEntity<ResponseDto<String>> accept(
            @Valid @RequestBody RoomRequestDTO request) {

        if (request.getRoomId() == null || request.getRoomId().trim().isEmpty()) {
            throw new BadRequestException("RoomId is required");
        }

        if (request.getSenderRole() != Role.PROPERTY_OWNER) {
            throw new BadRequestException("Only owner can accept chat");
        }

        chatService.acceptChat(request.getRoomId());
        return ResponseEntity.ok(
                new ResponseDto<>(200, "Chat accepted. Conversation started", request.getRoomId()));
    }


    @PostMapping("/reject")
    public ResponseEntity<ResponseDto<String>> reject(
            @Valid @RequestBody RoomRequestDTO request) {

        System.out.println("ROOM ID: " + request.getRoomId());
        System.out.println("ROLE: " + request.getSenderRole());

        if (request.getRoomId() == null || request.getRoomId().trim().isEmpty()) {
            throw new BadRequestException("RoomId is required");
        }

        if (request.getSenderRole() != Role.PROPERTY_OWNER) {
            throw new BadRequestException("Only owner can reject chat");
        }

        chatService.rejectChat(request.getRoomId());

        return ResponseEntity.ok(
                new ResponseDto<>(200, "Chat rejected successfully", request.getRoomId()));
    }

    @PostMapping("/typing")
            public ResponseEntity<ResponseDto<String>> typing(
            @RequestBody TypingDTO dto) {

        if (dto.getRoomId() == null) {
            throw new BadRequestException("RoomId is required");
        }
        chatService.handleTyping(dto);
        return ResponseEntity.ok(new ResponseDto<>(200, "Typing event sent", dto.getRoomId()));
    }

//    @PostMapping("/status")
//    public ResponseEntity<ResponseDto<String>> updateStatus(
//            @RequestParam Long userId,
//            @RequestParam boolean online) {
//
//        if (userId == null) {throw new BadRequestException("UserId is required");}
//
//        chatService.updateUserStatus(userId, online);
//        return ResponseEntity.ok(new ResponseDto<>(200, "User status updated", userId.toString()));
//    }

    @PostMapping("/status")
    public ResponseEntity<ResponseDto<String>> updateStatus(@RequestBody UserStatusDTO dto) {

        if (dto.getUserId() == null) {
            throw new BadRequestException("UserId is required");
        }

        chatService.updateUserStatus(dto.getUserId(), dto.isOnline());

        return ResponseEntity.ok(new ResponseDto<>(200, "status successfully", null));
    }

    @GetMapping("/history/{roomId}")
    public ResponseEntity<ResponseDto<List<MessageResponseDTO>>> getChatHistory(
            @PathVariable String roomId) {

        if (roomId == null || roomId.trim().isEmpty()) {
            throw new BadRequestException("RoomId is required");
        }

        List<Message> messages = messageRepo.findByRoomId(roomId);
        List<MessageResponseDTO> responseList = new ArrayList<>();

        for (Message msg : messages) {
            responseList.add(chatService.mapToDTO(msg));
        }
        return ResponseEntity.ok(new ResponseDto<>(200, "Chat history fetched successfully", responseList));
    }

    @GetMapping("/pending/{ownerId}")
    public ResponseEntity<ResponseDto<List<PendingChatDTO>>> getPendingChats(
            @PathVariable Long ownerId) {

        if (ownerId == null) {
            throw new BadRequestException("OwnerId is required");
        }

        List<PendingChatDTO> chats = chatService.getPendingChats(ownerId);

        return ResponseEntity.ok(
                new ResponseDto<>(200, "Pending chats fetched successfully", chats)
        );
    }

    @GetMapping("/accepted/{ownerId}")
    public ResponseEntity<ResponseDto<List<AcceptedChatDTO>>> getAcceptedChats(
            @PathVariable Long ownerId) {

        if (ownerId == null) {
            throw new BadRequestException("OwnerId is required");
        }

        List<AcceptedChatDTO> chats = chatService.getAcceptedChats(ownerId);
        return ResponseEntity.ok(new ResponseDto<>(200, "Accepted chats fetched successfully", chats));
    }

    @GetMapping("/rejected/{ownerId}")
    public ResponseEntity<ResponseDto<List<PendingChatDTO>>> getRejectedChats(
            @PathVariable Long ownerId) {
         if (ownerId == null) {
            throw new BadRequestException("OwnerId is required");
             }

         List<PendingChatDTO> chats = chatService.getRejectedChats(ownerId);
         return ResponseEntity.ok(new ResponseDto<>(200, "Rejected chats fetched successfully", chats));
    }

}
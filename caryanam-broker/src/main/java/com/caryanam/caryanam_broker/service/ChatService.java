package com.caryanam.caryanam_broker.service;


import com.caryanam.caryanam_broker.socket.*;

import java.util.List;

public interface ChatService {

    MessageResponseDTO sendMessage(MessageRequestDTO dto);

    String createOrGetRoom(Long userId, Long ownerId);

    void handleTyping(TypingDTO dto);

    void updateUserStatus(Long userId, boolean online);

    void acceptChat(String roomId);

    void rejectChat(String roomId);

    MessageResponseDTO mapToDTO(Message msg);

    List<PendingChatDTO> getPendingChats(Long ownerId);

    List<AcceptedChatDTO> getAcceptedChats(Long ownerId);

    List<PendingChatDTO> getRejectedChats(Long ownerId);

    List<MessageResponseDTO> getMessagesByRoom(String roomId);

}
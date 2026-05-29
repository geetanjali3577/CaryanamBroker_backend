package com.caryanam.caryanam_broker.socket;

import lombok.Data;

@Data
public class PendingChatDTO {

    private String roomId;
    private Long userId;
    private Long ownerId;
    private String lastMessage;
    private String time;

    public PendingChatDTO(String roomId, Long userId, Long ownerId, String lastMessage, String time) {
        this.roomId = roomId;
        this.userId = userId;
        this.ownerId = ownerId;
        this.lastMessage = lastMessage;
        this.time = time;
    }



}

package com.caryanam.caryanam_broker.socket;

import lombok.Data;

@Data
public class MessageRequestDTO {

    private Long userId;
    private Long ownerId;
    private String senderRole;
    private String message;
}

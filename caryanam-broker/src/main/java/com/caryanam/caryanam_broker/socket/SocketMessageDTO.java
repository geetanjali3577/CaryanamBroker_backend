package com.caryanam.caryanam_broker.socket;

import com.caryanam.caryanam_broker.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SocketMessageDTO {

    private Long id;
    private String roomId;
    private Long senderId;
    private String senderRole;
    private String content;
    private boolean isRead;
    private String timestamp;
    private MessageStatus status;
}
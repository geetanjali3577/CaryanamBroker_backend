package com.caryanam.caryanam_broker.socket;

import lombok.Data;

@Data
public class TypingDTO {
    private String roomId;
    private Long userId;
    private boolean typing;
}

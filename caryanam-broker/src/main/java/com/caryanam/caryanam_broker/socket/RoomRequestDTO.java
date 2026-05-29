package com.caryanam.caryanam_broker.socket;

import com.caryanam.caryanam_broker.enums.Role;

import lombok.Data;

@Data
public class RoomRequestDTO {

    private String roomId;
    private Long userId;
    private Long ownerId;
    private Role senderRole;


}
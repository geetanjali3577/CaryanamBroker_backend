package com.caryanam.caryanam_broker.socket;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponseDTO {

    private String roomId;
    private Long senderId;
    private String senderRole;
    private String message;
    private String time;


}

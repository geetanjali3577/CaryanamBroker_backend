package com.caryanam.caryanam_broker.socket;


import com.caryanam.caryanam_broker.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;

    private Long senderId;
    private String senderRole; // owner/ USER

    private String content;

   private boolean isRead;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;
}
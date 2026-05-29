package com.caryanam.caryanam_broker.repository;

import com.caryanam.caryanam_broker.enums.MessageStatus;
import com.caryanam.caryanam_broker.socket.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoomId(String roomId);

    Message findTopByRoomIdOrderByTimestampDesc(String roomId);
}

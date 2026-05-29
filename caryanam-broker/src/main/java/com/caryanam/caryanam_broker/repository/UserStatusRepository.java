package com.caryanam.caryanam_broker.repository;

import com.caryanam.caryanam_broker.socket.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
    boolean existsByUserIdAndOnlineTrue(Long userId);
}

package com.example.KTB_Agile_backend.chat.repository;

import com.example.KTB_Agile_backend.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
	long countByExchangeRequest_Id(Long exchangeRequestId);
	Optional<ChatRoom> findByExchangeRequest_Id(Long exchangeRequestId);
}

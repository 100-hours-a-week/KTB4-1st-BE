package com.example.KTB_Agile_backend.chat.repository;

import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findAllByChatRoom_IdOrderByIdDesc(Long chatRoomId, Pageable pageable);

	List<ChatMessage> findAllByChatRoom_IdAndIdLessThanOrderByIdDesc(
			Long chatRoomId,
			Long messageId,
			Pageable pageable
	);
}

package com.example.KTB_Agile_backend.ai.chatbot.repository;

import com.example.KTB_Agile_backend.ai.chatbot.entity.ChatbotMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
	List<ChatbotMessage> findAllByUser_IdOrderByIdDesc(Long userId, Pageable pageable);

	@Modifying
	@Query("delete from ChatbotMessage message where message.user.id = :userId and message.id < :oldestId")
	void deleteOlderThan(@Param("userId") Long userId, @Param("oldestId") Long oldestId);
}

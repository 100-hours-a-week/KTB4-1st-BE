package com.example.KTB_Agile_backend.chat.repository;

import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findAllByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

	List<ChatMessage> findAllByChatRoomIdAndIdLessThanOrderByIdDesc(
			Long chatRoomId,
			Long messageId,
			Pageable pageable
	);

	@Query("""
		select message
		from ChatMessage message
		where message.chatRoomId in :chatRoomIds
		  and message.id = (
			select max(latest.id)
			from ChatMessage latest
			where latest.chatRoomId = message.chatRoomId
		  )
		""")
	List<ChatMessage> findLatestByChatRoomIds(@Param("chatRoomIds") List<Long> chatRoomIds);
}

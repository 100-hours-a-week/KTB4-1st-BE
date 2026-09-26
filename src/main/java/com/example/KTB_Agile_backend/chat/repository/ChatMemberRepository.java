package com.example.KTB_Agile_backend.chat.repository;

import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
	long countByChatRoom_Id(Long chatRoomId);
}

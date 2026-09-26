package com.example.KTB_Agile_backend.chat.repository;

import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
	long countByChatRoom_Id(Long chatRoomId);

	@Query("""
		select member from ChatMember member
		where member.chatRoom.id = :chatRoomId
		  and member.chatRoom.deletedAt is null
		  and member.user.id = :userId
		  and member.user.deletedAt is null
		  and member.leftAt is null
		""")
	Optional<ChatMember> findActiveMember(
			@Param("chatRoomId") Long chatRoomId,
			@Param("userId") Long userId
	);
}

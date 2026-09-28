package com.example.KTB_Agile_backend.chat.repository;

import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
	long countByChatRoom_Id(Long chatRoomId);

	@Query("""
		select member from ChatMember member
		where member.chatRoom.id = :chatRoomId
		  and member.chatRoom.deletedAt is null
		  and member.user.id = :memberUserId
		  and member.user.deletedAt is null
		  and member.leftAt is null
		""")
	Optional<ChatMember> findActiveMember(
			@Param("chatRoomId") Long chatRoomId,
			@Param("memberUserId") Long userId
	);

	@Query("""
		select member
		from ChatMember member
		join fetch member.chatRoom room
		join fetch room.exchangeRequest request
		left join fetch request.group
		join fetch request.item item
		join fetch item.user
		join fetch request.requester
		where member.user.id = :userId
		  and member.user.deletedAt is null
		  and member.leftAt is null
		  and room.deletedAt is null
		  and (:direction is null
		       or (:direction = 'SENT' and request.requester.id = :userId)
		       or (:direction = 'RECEIVED' and request.requester.id <> :userId))
		order by room.lastMessageAt desc, room.id desc
		""")
	List<ChatMember> findChatRoomsByUserId(
			@Param("userId") Long userId,
			@Param("direction") String direction,
			Pageable pageable
	);

	@Query("""
		select member
		from ChatMember member
		join fetch member.chatRoom room
		join fetch room.exchangeRequest request
		left join fetch request.group
		join fetch request.item item
		join fetch item.user
		join fetch request.requester
		where member.user.id = :userId
		  and member.user.deletedAt is null
		  and member.leftAt is null
		  and room.deletedAt is null
		  and (:direction is null
		       or (:direction = 'SENT' and request.requester.id = :userId)
		       or (:direction = 'RECEIVED' and request.requester.id <> :userId))
		  and (room.lastMessageAt < :cursorAt
		       or (room.lastMessageAt = :cursorAt and room.id < :cursorRoomId))
		order by room.lastMessageAt desc, room.id desc
		""")
	List<ChatMember> findChatRoomsByUserIdAfter(
			@Param("userId") Long userId,
			@Param("direction") String direction,
			@Param("cursorAt") LocalDateTime cursorAt,
			@Param("cursorRoomId") Long cursorRoomId,
			Pageable pageable
	);

	@Query("""
		select member.chatRoom.id as chatRoomId, count(message.id) as unreadMessageCount
		from ChatMember member
		join ChatMessage message on message.chatRoom = member.chatRoom
		where member.user.id = :userId
		  and member.chatRoom.id in :chatRoomIds
		  and member.leftAt is null
		  and message.user.id <> :userId
		  and (member.lastReadMessageId is null or message.id > member.lastReadMessageId)
		group by member.chatRoom.id
		""")
	List<UnreadMessageCount> findUnreadMessageCounts(
			@Param("userId") Long userId,
			@Param("chatRoomIds") List<Long> chatRoomIds
	);

	interface UnreadMessageCount {
		Long getChatRoomId();

		Long getUnreadMessageCount();
	}
}

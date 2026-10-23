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
	long countByChatRoomId(Long chatRoomId);

	@Query(value = """
		select cm.*
		from chat_members cm
		join chat_rooms room on room.chat_room_id = cm.chat_room_id
		join users member_user on member_user.user_id = cm.user_id
		where room.chat_room_id = :chatRoomId
		  and room.deleted_at is null
		  and cm.user_id = :memberUserId
		  and member_user.deleted_at is null
		  and cm.left_at is null
		""", nativeQuery = true)
	Optional<ChatMember> findActiveMember(
			@Param("chatRoomId") Long chatRoomId,
			@Param("memberUserId") Long userId
	);

	@Query(value = """
		select cm.*
		from chat_members cm
		join chat_rooms room on room.chat_room_id = cm.chat_room_id
		join users member_user on member_user.user_id = cm.user_id
		join exchange_requests request on request.exchange_request_id = room.exchange_request_id
		where cm.user_id = :userId
		  and member_user.deleted_at is null
		  and cm.left_at is null
		  and room.deleted_at is null
		  and (:direction is null
		       or (:direction = 'SENT' and request.requester_id = :userId)
		       or (:direction = 'RECEIVED' and request.requester_id <> :userId))
		order by room.last_message_at desc, room.chat_room_id desc
		""", nativeQuery = true)
	List<ChatMember> findChatRoomsByUserId(
			@Param("userId") Long userId,
			@Param("direction") String direction,
			Pageable pageable
	);

	@Query(value = """
		select cm.*
		from chat_members cm
		join chat_rooms room on room.chat_room_id = cm.chat_room_id
		join users member_user on member_user.user_id = cm.user_id
		join exchange_requests request on request.exchange_request_id = room.exchange_request_id
		where cm.user_id = :userId
		  and member_user.deleted_at is null
		  and cm.left_at is null
		  and room.deleted_at is null
		  and (:direction is null
		       or (:direction = 'SENT' and request.requester_id = :userId)
		       or (:direction = 'RECEIVED' and request.requester_id <> :userId))
		  and (room.last_message_at < :cursorAt
		       or (room.last_message_at = :cursorAt and room.chat_room_id < :cursorRoomId))
		order by room.last_message_at desc, room.chat_room_id desc
		""", nativeQuery = true)
	List<ChatMember> findChatRoomsByUserIdAfter(
			@Param("userId") Long userId,
			@Param("direction") String direction,
			@Param("cursorAt") LocalDateTime cursorAt,
			@Param("cursorRoomId") Long cursorRoomId,
			Pageable pageable
	);

	@Query(value = """
		select cm.chat_room_id as chatRoomId, count(message.message_id) as unreadMessageCount
		from chat_members cm
		join chat_messages message on message.chat_room_id = cm.chat_room_id
		where cm.user_id = :userId
		  and cm.chat_room_id in (:chatRoomIds)
		  and cm.left_at is null
		  and (message.user_id is null or message.user_id <> :userId)
		  and (cm.last_read_message_id is null or message.message_id > cm.last_read_message_id)
		group by cm.chat_room_id
		""", nativeQuery = true)
	List<UnreadMessageCount> findUnreadMessageCounts(
			@Param("userId") Long userId,
			@Param("chatRoomIds") List<Long> chatRoomIds
	);

	interface UnreadMessageCount {
		Long getChatRoomId();

		Long getUnreadMessageCount();
	}
}

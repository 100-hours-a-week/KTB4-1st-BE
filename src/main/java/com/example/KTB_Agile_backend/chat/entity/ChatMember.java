package com.example.KTB_Agile_backend.chat.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "chat_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_members_id", nullable = false)
	private Long id;

	@Column(name = "chat_room_id", nullable = false)
	private Long chatRoomId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_role", nullable = false, length = 20)
	private ChatMemberRole memberRole;

	@Column(name = "left_at")
	private LocalDateTime leftAt;

	@Column(name = "last_read_message_id")
	private Long lastReadMessageId;

	public ChatMember(Long chatRoomId, Long userId, ChatMemberRole memberRole) {
		this.chatRoomId = requireNonNull(chatRoomId, "chatRoomId must not be null");
		this.userId = requireNonNull(userId, "userId must not be null");
		this.memberRole = requireNonNull(memberRole, "memberRole must not be null");
	}

	public void markReadThrough(Long messageId) {
		if (messageId != null && (lastReadMessageId == null || messageId > lastReadMessageId)) {
			lastReadMessageId = messageId;
		}
	}

	public void leave(LocalDateTime leftAt) {
		this.leftAt = requireNonNull(leftAt, "leftAt must not be null");
	}
}

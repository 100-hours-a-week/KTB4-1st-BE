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

import java.util.Objects;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "message_id", nullable = false)
	private Long id;

	@Column(name = "chat_room_id", nullable = false)
	private Long chatRoomId;

	@Column(name = "user_id")
	private Long userId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type", nullable = false, length = 20)
	private ChatMessageType messageType = ChatMessageType.TEXT;

	@Enumerated(EnumType.STRING)
	@Column(name = "system_event_type", length = 40)
	private ChatSystemEventType systemEventType;

	public ChatMessage(Long chatRoomId, Long userId, String content) {
		this.chatRoomId = Objects.requireNonNull(chatRoomId, "chatRoomId must not be null");
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.content = content;
	}

	public static ChatMessage system(Long chatRoomId, ChatSystemEventType eventType) {
		ChatMessage message = new ChatMessage();
		message.chatRoomId = Objects.requireNonNull(chatRoomId, "chatRoomId must not be null");
		message.content = eventType.content();
		message.messageType = ChatMessageType.SYSTEM;
		message.systemEventType = eventType;
		return message;
	}
}

package com.example.KTB_Agile_backend.chat.document;

import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatMessageType;
import com.example.KTB_Agile_backend.chat.entity.ChatMessageView;
import com.example.KTB_Agile_backend.chat.entity.ChatSystemEventType;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Document("chat_messages")
public class ChatMessageDocument implements ChatMessageView {

	@MongoId(FieldType.OBJECT_ID)
	private String objectId;

	@Field("message_id")
	private Long messageId;

	@Field("chat_room_id")
	private Long chatRoomId;

	@Field("user_id")
	private Long userId;

	private String content;

	@Field("message_type")
	private ChatMessageType messageType;

	@Field("system_event_type")
	private ChatSystemEventType systemEventType;

	@Field("created_at")
	private LocalDateTime createdAt;

	protected ChatMessageDocument() {
	}

	private ChatMessageDocument(ChatMessage message) {
		this.objectId = message.getMessageObjectId();
		this.messageId = message.getId();
		this.chatRoomId = message.getChatRoomId();
		this.userId = message.getUserId();
		this.content = message.getContent();
		this.messageType = message.getMessageType();
		this.systemEventType = message.getSystemEventType();
		this.createdAt = message.getCreatedAt();
	}

	public static ChatMessageDocument from(ChatMessage message) {
		return new ChatMessageDocument(message);
	}

	@Override
	public Long getId() {
		return messageId;
	}

	@Override
	public Long getChatRoomId() {
		return chatRoomId;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public ChatMessageType getMessageType() {
		return messageType;
	}

	@Override
	public ChatSystemEventType getSystemEventType() {
		return systemEventType;
	}

	@Override
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

package com.example.KTB_Agile_backend.chat.entity;

import java.time.LocalDateTime;

public interface ChatMessageView {
	Long getId();

	Long getChatRoomId();

	Long getUserId();

	String getContent();

	ChatMessageType getMessageType();

	ChatSystemEventType getSystemEventType();

	LocalDateTime getCreatedAt();
}

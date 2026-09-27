package com.example.KTB_Agile_backend.chat.dto.response;

import com.example.KTB_Agile_backend.chat.entity.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
		Long messageId,
		Long chatRoomId,
		Long userId,
		String content,
		ChatMessageType messageType,
		LocalDateTime createdAt
) {
}

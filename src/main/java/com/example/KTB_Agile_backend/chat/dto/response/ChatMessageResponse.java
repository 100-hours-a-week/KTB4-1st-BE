package com.example.KTB_Agile_backend.chat.dto.response;

import com.example.KTB_Agile_backend.chat.entity.ChatMessageType;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(
		Long messageId,
		Long chatRoomId,
		Long userId,
		String content,
		ChatMessageType messageType,
		LocalDateTime createdAt,
		UserStatus userStatus,
		GroupMemberStatus groupMemberStatus,
		Payload payload
) {
	public record Payload(
			String eventType,
			Long exchangeRequestId,
			RequestedItem requestedItem,
			Integer requestedQuantity,
			List<OfferedItem> offeredItems
	) {
	}

	public record RequestedItem(
			Long itemId,
			String title,
			Integer availableQuantity,
			String thumbnailImageUrl
	) {
	}

	public record OfferedItem(
			Long itemId,
			String title,
			Integer offeredQuantity,
			Integer availableQuantity,
			String thumbnailImageUrl
	) {
	}
}

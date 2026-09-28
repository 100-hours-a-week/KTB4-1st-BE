package com.example.KTB_Agile_backend.chat.dto.response;

import com.example.KTB_Agile_backend.chat.entity.ChatRoomStatus;

import java.time.OffsetDateTime;

public record ChatRoomSummary(
		Long chatRoomId,
		ChatRoomStatus chatRoomStatus,
		Long exchangeRequestId,
		String direction,
		GroupInfo group,
		OtherUser otherUser,
		TargetItem targetItem,
		LastMessage lastMessage,
		long unreadMessageCount,
		OffsetDateTime lastMessageAt
) {
	public record GroupInfo(Long groupId, String groupName) {
	}

	public record OtherUser(Long userId, String nickname, String profileImageUrl) {
	}

	public record TargetItem(Long itemId, String title, String thumbnailImageUrl) {
	}

	public record LastMessage(Long messageId, String content, Long senderId, OffsetDateTime createdAt) {
	}
}

package com.example.KTB_Agile_backend.chat.dto.response;

import com.example.KTB_Agile_backend.chat.entity.ChatRoomStatus;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomSummary(
		Long chatRoomId,
		ChatRoomStatus chatRoomStatus,
		Long exchangeRequestId,
		ExchangeRequestStatus exchangeStatus,
		String direction,
		GroupInfo group,
		OtherUser otherUser,
		TargetItem targetItem,
		OfferedItem offeredItem,
		LastMessage lastMessage,
		long unreadMessageCount,
		LocalDateTime lastMessageAt
) {
	public record GroupInfo(Long groupId, String groupName) {
	}

	public record OtherUser(
			Long userId,
			String nickname,
			UserStatus userStatus,
			GroupMemberStatus groupMemberStatus
	) {
	}

	public record TargetItem(Long itemId, String title, String thumbnailImageUrl) {
	}

	public record OfferedItem(Long itemId, String title, String thumbnailImageUrl) {
	}

	public record LastMessage(Long messageId, String content, Long senderId, LocalDateTime createdAt) {
	}
}

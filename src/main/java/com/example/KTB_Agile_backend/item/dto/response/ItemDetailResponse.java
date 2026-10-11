package com.example.KTB_Agile_backend.item.dto.response;

import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.user.entity.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ItemDetailResponse(
		Long itemId,
		List<GroupInfo> groups,
		String title,
		String content,
		Integer quantity,
		ItemState itemState,
		Owner owner,
		List<ImageInfo> images,
		Long thumbnailImageId,
		Long likeCount,
		Long viewCount,
		Long exchangeRequestCount,
		Boolean isLiked,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		BigDecimal exchangeUrgencyScore,
		BigDecimal valueGapToleranceScore
) {

	public ItemDetailResponse {
		groups = List.copyOf(groups);
		images = List.copyOf(images);
	}

	public record GroupInfo(
			Long groupId,
			String groupName,
			GroupMemberStatus ownerMembershipStatus
	) {
	}

	public record Owner(
			Long userId,
			String nickname,
			String profileImageUrl,
			UserStatus userStatus
	) {
	}

	public record ImageInfo(
			Long imageId,
			String imageUrl,
			Integer displayOrder
	) {
	}
}

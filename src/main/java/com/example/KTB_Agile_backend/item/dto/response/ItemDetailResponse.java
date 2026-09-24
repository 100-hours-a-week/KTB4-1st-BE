package com.example.KTB_Agile_backend.item.dto.response;

import com.example.KTB_Agile_backend.item.entity.ItemState;

import java.time.OffsetDateTime;
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
		Long likeCount,
		Long viewCount,
		Long exchangeRequestCount,
		Boolean isLiked,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {

	public ItemDetailResponse {
		groups = List.copyOf(groups);
		images = List.copyOf(images);
	}

	public record GroupInfo(
			Long groupId,
			String groupName
	) {
	}

	public record Owner(
			Long userId,
			String nickname,
			String profileImageUrl
	) {
	}

	public record ImageInfo(
			Long imageId,
			String imageUrl,
			Integer displayOrder
	) {
	}
}

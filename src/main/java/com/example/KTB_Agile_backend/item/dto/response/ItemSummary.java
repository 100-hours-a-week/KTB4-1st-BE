package com.example.KTB_Agile_backend.item.dto.response;

import com.example.KTB_Agile_backend.item.entity.ItemState;

import java.time.OffsetDateTime;

public record ItemSummary(
		Long itemId,
		String title,
		String contentPreview,
		Integer quantity,
		Owner owner,
		ItemState itemState,
		String thumbnailImageUrl,
		Long likeCount,
		Long exchangeRequestCount,
		Boolean isLiked,
		OffsetDateTime createdAt
) {

	public record Owner(
			Long userId,
			String nickname
	) {
	}
}

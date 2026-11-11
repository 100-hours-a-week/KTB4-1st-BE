package com.example.KTB_Agile_backend.item.cache;

import com.example.KTB_Agile_backend.item.entity.ItemState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemDetailCacheValue(
		Long ownerUserId,
		String title,
		String content,
		Integer quantity,
		ItemState itemState,
		Long thumbnailImageId,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		BigDecimal exchangeUrgencyScore,
		BigDecimal valueGapToleranceScore
) {
}

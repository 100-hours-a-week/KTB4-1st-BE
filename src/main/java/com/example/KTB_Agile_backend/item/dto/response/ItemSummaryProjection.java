package com.example.KTB_Agile_backend.item.dto.response;

import com.example.KTB_Agile_backend.item.entity.ItemState;

import java.time.LocalDateTime;

public record ItemSummaryProjection(
		Long itemId,
		String title,
		String content,
		Integer quantity,
		Long ownerId,
		String ownerNickname,
		ItemState itemState,
		LocalDateTime createdAt
) {
}

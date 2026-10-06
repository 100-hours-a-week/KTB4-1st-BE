package com.example.KTB_Agile_backend.item.dto.response;

import java.time.LocalDateTime;

public record ItemLikeResponse(
		Long itemId,
		Boolean isLiked,
		Long likeCount,
		LocalDateTime createdAt
) {
}

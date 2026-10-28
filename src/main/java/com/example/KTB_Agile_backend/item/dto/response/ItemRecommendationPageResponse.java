package com.example.KTB_Agile_backend.item.dto.response;

import java.util.List;

public record ItemRecommendationPageResponse(
		List<ItemSummary> items,
		String nextCursor,
		boolean hasNext
) {

	public ItemRecommendationPageResponse {
		items = List.copyOf(items);
	}
}

package com.example.KTB_Agile_backend.item.dto.response;

import java.util.List;

public record ItemPageResponse(
		List<ItemSummary> items,
		String nextCursor,
		boolean hasNext
) {

	public ItemPageResponse {
		items = List.copyOf(items);
	}
}

package com.example.KTB_Agile_backend.search.dto.response;

import java.util.List;

public record SearchHistoryPageResponse(
		List<SearchHistorySummary> searchHistories,
		String nextCursor,
		boolean hasNext
) {

	public SearchHistoryPageResponse {
		searchHistories = List.copyOf(searchHistories);
	}
}

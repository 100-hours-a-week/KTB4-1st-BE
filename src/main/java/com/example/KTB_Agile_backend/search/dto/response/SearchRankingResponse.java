package com.example.KTB_Agile_backend.search.dto.response;

import java.time.LocalDate;
import java.util.List;

public record SearchRankingResponse(
		LocalDate date,
		List<SearchRankingEntry> rankings
) {

	public SearchRankingResponse {
		rankings = List.copyOf(rankings);
	}
}

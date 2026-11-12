package com.example.KTB_Agile_backend.search.dto.response;

public record SearchRankingEntry(
		int rank,
		String keyword,
		long searchCount
) {
}

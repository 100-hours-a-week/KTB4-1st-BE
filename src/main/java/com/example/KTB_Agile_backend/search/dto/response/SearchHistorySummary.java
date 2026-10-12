package com.example.KTB_Agile_backend.search.dto.response;

import java.time.LocalDateTime;

public record SearchHistorySummary(
		Long searchHistoryId,
		String keyword,
		LocalDateTime lastSearchedAt,
		LocalDateTime createdAt
) {
}

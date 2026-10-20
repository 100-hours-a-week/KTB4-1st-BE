package com.example.KTB_Agile_backend.item.event;

import java.math.BigDecimal;

public record ItemPriceEstimationRequested(
		String eventId,
		String requestId,
		String originalRequestEventId,
		Long itemId,
		String keyword,
		String title,
		String content,
		BigDecimal valueGapToleranceScore,
		BigDecimal exchangeUrgencyScore,
		int attemptCount
) {
	public ItemPriceEstimationRequested retry(String retryEventId, int retryAttemptCount) {
		return new ItemPriceEstimationRequested(retryEventId, requestId, originalRequestEventId, itemId, keyword,
				title, content, valueGapToleranceScore, exchangeUrgencyScore, retryAttemptCount);
	}
}

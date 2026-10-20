package com.example.KTB_Agile_backend.item.price.messaging;

import java.time.LocalDateTime;

public record ItemPriceEstimationFailed(
		String requestId,
		String originalRequestEventId,
		String failedAttemptEventId,
		Long itemId,
		ItemPriceEstimationRequested requestSnapshot,
		int attemptCount,
		String failureClassification,
		LocalDateTime failedAt
) {
	public static ItemPriceEstimationFailed from(ItemPriceEstimationRequested request, String failureClassification,
			LocalDateTime failedAt) {
		ItemPriceEstimationRequested snapshot = new ItemPriceEstimationRequested(
				request.originalRequestEventId(), request.requestId(), request.originalRequestEventId(),
				request.itemId(), request.keyword(), request.title(), request.content(),
				request.valueGapToleranceScore(), request.exchangeUrgencyScore(), 1);
		return new ItemPriceEstimationFailed(request.requestId(), request.originalRequestEventId(), request.eventId(),
				request.itemId(), snapshot, request.attemptCount(), failureClassification, failedAt);
	}
}

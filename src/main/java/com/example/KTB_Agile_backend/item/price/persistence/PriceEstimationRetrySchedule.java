package com.example.KTB_Agile_backend.item.price.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_price_estimation_retry_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceEstimationRetrySchedule {
	@Id
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	@Column(name = "original_request_event_id", nullable = false, length = 36)
	private String originalRequestEventId;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at")
	private LocalDateTime nextAttemptAt;

	@Column(name = "validation_retry_used", nullable = false)
	private boolean validationRetryUsed;

	public PriceEstimationRetrySchedule(String requestId, String originalRequestEventId, int attemptCount,
			LocalDateTime nextAttemptAt, boolean validationRetryUsed) {
		this.requestId = requestId;
		this.originalRequestEventId = originalRequestEventId;
		this.attemptCount = attemptCount;
		this.nextAttemptAt = nextAttemptAt;
		this.validationRetryUsed = validationRetryUsed;
	}

	public boolean isCurrentAttempt(int attempt) {
		return attemptCount == attempt && nextAttemptAt == null;
	}

	public boolean isDueAt(LocalDateTime now) {
		return nextAttemptAt != null && !nextAttemptAt.isAfter(now);
	}

	public void schedule(int nextAttempt, LocalDateTime scheduledAt, boolean validationRetryUsed) {
		this.attemptCount = nextAttempt;
		this.nextAttemptAt = scheduledAt;
		this.validationRetryUsed = validationRetryUsed;
	}

	@SuppressWarnings("PMD.NullAssignment")
	public void markPublished() {
		this.nextAttemptAt = null;
	}
}

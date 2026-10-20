package com.example.KTB_Agile_backend.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class PriceEstimationRetryPolicy {
	private final PriceEstimationRetryProperties properties;

	public Duration delayAfterFailure(int failedAttempt) {
		double scaled = properties.baseDelay().toMillis()
				* Math.pow(properties.multiplier(), Math.max(0, failedAttempt - 1));
		long capMillis = Math.min(properties.maxDelay().toMillis(), (long) Math.min(scaled, Long.MAX_VALUE));
		return Duration.ofMillis(properties.fullJitter()
				? ThreadLocalRandom.current().nextLong(capMillis + 1)
				: capMillis);
	}

	public boolean shouldRetry(int failedAttempt, boolean validationRetryUsed, Failure failure) {
		if (!properties.enabled() || failedAttempt >= properties.maxAttempts()) return false;
		return switch (failure) {
			case TRANSIENT -> true;
			case RESPONSE_VALIDATION -> properties.retryResponseValidation() && !validationRetryUsed;
			case INPUT, CONFIGURATION -> false;
		};
	}

	public enum Failure { TRANSIENT, RESPONSE_VALIDATION, INPUT, CONFIGURATION }
}

package com.example.KTB_Agile_backend.item.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PriceEstimationRetryPolicyTest {
	private final PriceEstimationRetryPolicy policy = new PriceEstimationRetryPolicy(properties(true, 4, true,
			Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), true));

	@Test
	void retriesTransientAndOnlyOneValidationFailureWithinFourAttempts() {
		assertThat(policy.shouldRetry(1, false, PriceEstimationRetryPolicy.Failure.TRANSIENT)).isTrue();
		assertThat(policy.shouldRetry(1, false, PriceEstimationRetryPolicy.Failure.RESPONSE_VALIDATION)).isTrue();
		assertThat(policy.shouldRetry(2, true, PriceEstimationRetryPolicy.Failure.RESPONSE_VALIDATION)).isFalse();
		assertThat(policy.shouldRetry(4, false, PriceEstimationRetryPolicy.Failure.TRANSIENT)).isFalse();
		assertThat(policy.shouldRetry(1, false, PriceEstimationRetryPolicy.Failure.INPUT)).isFalse();
		assertThat(policy.shouldRetry(1, false, PriceEstimationRetryPolicy.Failure.CONFIGURATION)).isFalse();
	}

	@Test
	void fullJitterDelayRespectsExponentialCaps() {
		assertThat(policy.delayAfterFailure(1).toMillis()).isBetween(0L, 1_000L);
		assertThat(policy.delayAfterFailure(2).toMillis()).isBetween(0L, 2_000L);
		assertThat(policy.delayAfterFailure(3).toMillis()).isBetween(0L, 4_000L);
		assertThat(policy.delayAfterFailure(4).toMillis()).isBetween(0L, 5_000L);
	}

	@Test
	void retryValuesAndAutomaticRetryCanBeChangedThroughPolicyConfiguration() {
		var changed = new PriceEstimationRetryPolicy(properties(true, 3, false,
				Duration.ofMillis(250), 3.0, Duration.ofSeconds(2), false));
		var disabled = new PriceEstimationRetryPolicy(properties(false, 3, true,
				Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), true));

		assertThat(changed.delayAfterFailure(2)).isEqualTo(Duration.ofMillis(750));
		assertThat(changed.delayAfterFailure(4)).isEqualTo(Duration.ofSeconds(2));
		assertThat(disabled.shouldRetry(1, false, PriceEstimationRetryPolicy.Failure.TRANSIENT)).isFalse();
	}

	private static PriceEstimationRetryProperties properties(boolean enabled, int attempts, boolean retryResponseValidation,
			Duration base, double multiplier, Duration cap, boolean jitter) {
		return new PriceEstimationRetryProperties(enabled, attempts, retryResponseValidation, base, multiplier, cap,
				jitter, Duration.ofMillis(500));
	}
}

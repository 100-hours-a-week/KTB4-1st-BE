package com.example.KTB_Agile_backend.item.price.config;

import com.example.KTB_Agile_backend.item.price.config.PriceEstimationRetryProperties;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.item-price-estimation.retry")
public record PriceEstimationRetryProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("4") int maxAttempts,
		@DefaultValue("true") boolean retryResponseValidation,
		@DefaultValue("1s") Duration baseDelay,
		@DefaultValue("2.0") double multiplier,
		@DefaultValue("5s") Duration maxDelay,
		@DefaultValue("true") boolean fullJitter,
		@DefaultValue("500ms") Duration pollInterval
) {
	public PriceEstimationRetryProperties {
		if (maxAttempts < 1 || baseDelay.isNegative() || !Double.isFinite(multiplier) || multiplier < 1
				|| maxDelay.isNegative() || maxDelay.toMillis() == Long.MAX_VALUE
				|| pollInterval.isNegative() || pollInterval.isZero()) {
			throw new IllegalArgumentException("Invalid item price estimation retry configuration");
		}
	}
}

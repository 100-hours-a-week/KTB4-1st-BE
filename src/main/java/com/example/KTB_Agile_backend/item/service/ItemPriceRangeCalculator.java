package com.example.KTB_Agile_backend.item.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ItemPriceRangeCalculator {
	private static final BigDecimal K_SPREAD = new BigDecimal("0.19");
	private static final BigDecimal K_SKEW = new BigDecimal("0.15");

	public PriceRange calculate(
			Long unitPrice,
			BigDecimal valueGapToleranceScore,
			BigDecimal exchangeUrgencyScore
	) {
		BigDecimal discountRatio = valueGapToleranceScore.multiply(K_SPREAD)
				.add(exchangeUrgencyScore.multiply(K_SKEW));
		Long minUnitPrice = BigDecimal.valueOf(unitPrice)
				.multiply(BigDecimal.ONE.subtract(discountRatio))
				.setScale(0, RoundingMode.HALF_UP)
				.longValueExact();
		return new PriceRange(unitPrice, minUnitPrice, unitPrice);
	}

	public record PriceRange(Long unitPrice, Long minUnitPrice, Long maxUnitPrice) {
	}
}

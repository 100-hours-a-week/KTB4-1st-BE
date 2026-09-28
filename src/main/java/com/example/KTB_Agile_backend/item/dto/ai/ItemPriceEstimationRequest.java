package com.example.KTB_Agile_backend.item.dto.ai;

import java.math.BigDecimal;

public record ItemPriceEstimationRequest(
		String title,
		String content,
		BigDecimal valueGapToleranceScore,
		BigDecimal tradeUrgencyScore
) {
}

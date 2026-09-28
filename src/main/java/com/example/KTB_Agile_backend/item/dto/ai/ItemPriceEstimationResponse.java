package com.example.KTB_Agile_backend.item.dto.ai;

public record ItemPriceEstimationResponse(
		String keyword,
		Long unitPrice,
		Long minUnitPrice,
		Long maxUnitPrice
) {
}

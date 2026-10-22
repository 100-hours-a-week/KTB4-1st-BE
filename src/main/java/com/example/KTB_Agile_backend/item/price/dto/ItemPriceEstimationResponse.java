package com.example.KTB_Agile_backend.item.price.dto;
public record ItemPriceEstimationResponse(
		String keyword,
		Long unitPrice,
		Long minUnitPrice,
		Long maxUnitPrice
) {
}

package com.example.KTB_Agile_backend.exchange.dto.response;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;

import java.util.List;

public record ExchangeRequestEditResponse(
		Long exchangeRequestId,
		Long itemId,
		Integer requestedQuantity,
		List<OfferedItemResponse> offeredItems,
		ExchangeRequestStatus requestedStatus
) {

	public ExchangeRequestEditResponse {
		offeredItems = List.copyOf(offeredItems);
	}

	public record OfferedItemResponse(Long itemId, Integer quantity) {
	}
}

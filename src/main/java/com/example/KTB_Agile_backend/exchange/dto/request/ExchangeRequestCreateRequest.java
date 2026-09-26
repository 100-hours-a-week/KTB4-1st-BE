package com.example.KTB_Agile_backend.exchange.dto.request;

import java.util.List;

public record ExchangeRequestCreateRequest(Integer requestedQuantity, List<OfferedItemRequest> offeredItems) {

	public record OfferedItemRequest(Long itemId, Integer quantity) {
	}
}

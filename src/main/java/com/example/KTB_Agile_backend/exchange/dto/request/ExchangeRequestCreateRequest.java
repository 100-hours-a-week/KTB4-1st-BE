package com.example.KTB_Agile_backend.exchange.dto.request;

import java.util.List;

public record ExchangeRequestCreateRequest(
		Long groupId,
		Integer requestedQuantity,
		List<OfferedItemRequest> offeredItems
) {

	public ExchangeRequestCreateRequest(Integer requestedQuantity, List<OfferedItemRequest> offeredItems) {
		this(null, requestedQuantity, offeredItems);
	}

	public record OfferedItemRequest(Long itemId, Integer quantity) {
	}
}

package com.example.KTB_Agile_backend.exchange.dto.response;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ExchangeRequestCreatedResponse(
		Long exchangeRequestId,
		Long itemId,
		Integer requestedQuantity,
		List<OfferedItemResponse> offeredItems,
		ExchangeRequestStatus requestedStatus,
		Long chatRoomId,
		LocalDateTime createdAt
) {
}

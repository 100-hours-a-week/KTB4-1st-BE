package com.example.KTB_Agile_backend.exchange.dto.response;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;

import java.time.OffsetDateTime;

public record ExchangeRequestStatusResponse(
		Long exchangeRequestId,
		Long itemId,
		ExchangeRequestStatus status,
		OffsetDateTime updatedAt
) {
}

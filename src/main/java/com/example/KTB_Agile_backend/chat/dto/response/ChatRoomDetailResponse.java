package com.example.KTB_Agile_backend.chat.dto.response;

import com.example.KTB_Agile_backend.chat.entity.ChatRoomStatus;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;

import java.util.List;

public record ChatRoomDetailResponse(
		Long chatRoomId,
		ChatRoomStatus chatRoomStatus,
		ExchangeRequestInfo exchangeRequest
) {
	public record ExchangeRequestInfo(
			Long exchangeRequestId,
			ExchangeRequestStatus exchangeStatus,
			RequestedItem requestedItem,
			Integer requestedQuantity,
			List<OfferedItem> offeredItems
	) {
		public ExchangeRequestInfo {
			offeredItems = List.copyOf(offeredItems);
		}
	}

	public record RequestedItem(
			Long itemId,
			String title,
			Integer availableQuantity,
			String thumbnailImageUrl
	) {
	}

	public record OfferedItem(
			Long itemId,
			String title,
			Integer offeredQuantity,
			Integer availableQuantity,
			String thumbnailImageUrl
	) {
	}
}

package com.example.KTB_Agile_backend.exchange.dto.response;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.entity.ItemState;

import java.time.LocalDateTime;
import java.util.List;

public record CompletedExchangePageResponse(
		List<CompletedExchange> items,
		String nextCursor,
		boolean hasNext
) {

	public CompletedExchangePageResponse {
		items = List.copyOf(items);
	}

	public record CompletedExchange(
			Long exchangeRequestId,
			ExchangeRequestStatus exchangeStatus,
			LocalDateTime exchangedAt,
			ExchangedItem requestedItem,
			List<ExchangedItem> offeredItems
	) {

		public CompletedExchange {
			offeredItems = List.copyOf(offeredItems);
		}
	}

	public record ExchangedItem(
			Long itemId,
			String title,
			ItemSummary.Owner owner,
			Integer quantity,
			ItemState itemState,
			String thumbnailImageUrl
	) {
	}
}

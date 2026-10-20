package com.example.KTB_Agile_backend.item.price.application;

import com.example.KTB_Agile_backend.item.price.cache.ItemPriceCache;
import com.example.KTB_Agile_backend.item.price.dto.ItemPriceEstimationResponse;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetryScheduleRepository;
import com.example.KTB_Agile_backend.item.price.exception.PriceEstimationException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ItemPriceResultServiceTest {
	@Test
	void conditionallyAppliesZeroPriceAndCachesAfterCommit() {
		ItemRepository items = mock(ItemRepository.class);
		PriceEstimationRetryScheduleRepository schedules = mock(PriceEstimationRetryScheduleRepository.class);
		when(schedules.findByRequestIdForUpdate("request"))
				.thenReturn(java.util.Optional.empty(), java.util.Optional.empty());
		when(items.findPendingPriceRequestForUpdate(7L, "request"))
				.thenReturn(java.util.Optional.of(mock(Item.class)));
		ItemPriceCache cache = mock(ItemPriceCache.class);
		when(items.completePriceRequest(7L, "request", 0L, 0L, 0L)).thenReturn(1);
		var service = new ItemPriceResultService(items, schedules, cache);
		var request = new ItemPriceEstimationRequested("event", "request", "event", 7L, " book ",
				"title", "content", new BigDecimal("0.30"), new BigDecimal("0.50"), 1);
		TransactionSynchronizationManager.initSynchronization();
		try {
			assertThat(service.apply(request, new ItemPriceEstimationResponse("book", 0L, 0L, 0L))).isTrue();
			verify(cache, never()).put(anyString(), anyLong(), any());
			TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
			verify(cache).put("book", 0L, java.time.Duration.ofHours(24));
			verify(schedules, never()).delete(any());
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void rejectsInvalidPricesWithoutUpdatingItem() {
		ItemRepository items = mock(ItemRepository.class);
		PriceEstimationRetryScheduleRepository schedules = mock(PriceEstimationRetryScheduleRepository.class);
		when(schedules.findByRequestIdForUpdate("request"))
				.thenReturn(java.util.Optional.empty(), java.util.Optional.empty());
		when(items.findPendingPriceRequestForUpdate(7L, "request"))
				.thenReturn(java.util.Optional.of(mock(Item.class)));
		var service = new ItemPriceResultService(items, schedules, mock(ItemPriceCache.class));
		var request = new ItemPriceEstimationRequested("event", "request", "event", 7L, "book",
				"title", "content", BigDecimal.ZERO, BigDecimal.ZERO, 1);
		assertThrows(PriceEstimationException.class,
				() -> service.apply(request, new ItemPriceEstimationResponse("book", 2L, 3L, 1L)));
		verify(items, never()).completePriceRequest(any(), any(), any(), any(), any());
	}
}

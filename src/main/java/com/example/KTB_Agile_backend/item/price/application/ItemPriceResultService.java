package com.example.KTB_Agile_backend.item.price.application;

import com.example.KTB_Agile_backend.item.price.cache.ItemPriceCache;
import com.example.KTB_Agile_backend.item.price.dto.ItemPriceEstimationResponse;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetrySchedule;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetryScheduleRepository;
import com.example.KTB_Agile_backend.item.price.exception.PriceEstimationException;
import com.example.KTB_Agile_backend.item.price.PriceEstimationFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemPriceResultService {
	private static final Duration CACHE_TTL = Duration.ofHours(24);
	private static final int UPDATED_ROW_COUNT = 1;
	private final ItemRepository itemRepository;
	private final PriceEstimationRetryScheduleRepository retryScheduleRepository;
	private final ItemPriceCache itemPriceCache;

	@Transactional
	public boolean apply(ItemPriceEstimationRequested request, ItemPriceEstimationResponse response) {
		PriceEstimationRetrySchedule schedule = retryScheduleRepository.findByRequestIdForUpdate(request.requestId())
				.orElse(null);
		if (schedule == null && request.attemptCount() == 1) {
			if (itemRepository.findPendingPriceRequestForUpdate(request.itemId(), request.requestId()).isEmpty()) {
				return false;
			}
			schedule = retryScheduleRepository.findByRequestIdForUpdate(request.requestId()).orElse(null);
		}
		if (schedule == null ? request.attemptCount() != 1 : !schedule.isCurrentAttempt(request.attemptCount())) {
			return false;
		}
		if (response == null || response.keyword() == null
				|| response.unitPrice() == null || response.minUnitPrice() == null || response.maxUnitPrice() == null
				|| response.minUnitPrice() < 0 || response.minUnitPrice() > response.unitPrice()
				|| response.unitPrice() > response.maxUnitPrice()) {
			throw new PriceEstimationException(PriceEstimationFailure.RESPONSE_VALIDATION);
		}
		int updated = itemRepository.completePriceRequest(request.itemId(), request.requestId(), response.unitPrice(),
				response.minUnitPrice(), response.maxUnitPrice());
		if (updated == UPDATED_ROW_COUNT) {
			if (schedule != null) retryScheduleRepository.delete(schedule);
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					if (request.keyword() == null || request.keyword().isBlank()) return;
					try {
						itemPriceCache.put(request.keyword().strip(), response.unitPrice(), CACHE_TTL);
					} catch (RuntimeException exception) {
						if (log.isWarnEnabled()) {
							log.warn("Could not cache committed item price for request {}", request.requestId(), exception);
						}
					}
				}
			});
		}
		return updated == UPDATED_ROW_COUNT;
	}
}

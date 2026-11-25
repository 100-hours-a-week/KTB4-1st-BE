package com.example.KTB_Agile_backend.item.price.application;

import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.price.client.ItemPriceEstimator;
import com.example.KTB_Agile_backend.item.price.exception.PriceEstimationException;
import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationInboxRepository;
import com.example.KTB_Agile_backend.item.price.retry.PriceEstimationRetryScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ItemPriceEstimationProcessor {
	private final ItemPriceEstimator itemPriceEstimator;
	private final ItemPriceResultService itemPriceResultService;
	private final PriceEstimationRetryScheduler retryScheduler;
	private final ItemPriceEstimationInboxRepository inboxRepository;
	private final PlatformTransactionManager transactionManager;

	public void process(ItemPriceEstimationRequested request) {
		String eventId = request.eventId();
		if (eventId == null || eventId.isBlank()) {
			throw new IllegalArgumentException("Item price estimation eventId must not be blank");
		}
		if (inboxRepository.exists(eventId)) return;
		if (!retryScheduler.isCurrentAttempt(request)) {
			markProcessed(eventId);
			return;
		}
		try {
			var response = itemPriceEstimator.estimate(request);
			inTransaction(() -> {
				itemPriceResultService.apply(request, response);
				inboxRepository.markProcessed(eventId);
			});
		} catch (PriceEstimationException exception) {
			inTransaction(() -> {
				retryScheduler.recordFailure(request, exception.failure());
				inboxRepository.markProcessed(eventId);
			});
		}
	}

	private void markProcessed(String eventId) {
		inTransaction(() -> inboxRepository.markProcessed(eventId));
	}

	private void inTransaction(Runnable action) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
	}
}

package com.example.KTB_Agile_backend.item.price.application;

import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.price.ItemCashService;
import com.example.KTB_Agile_backend.item.price.exception.PriceEstimationException;
import com.example.KTB_Agile_backend.item.price.retry.PriceEstimationRetryScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemPriceEstimationProcessor {
	private final ItemCashService itemCashService;
	private final ItemPriceResultService itemPriceResultService;
	private final PriceEstimationRetryScheduler retryScheduler;

	public void process(ItemPriceEstimationRequested request) {
		if (!retryScheduler.isCurrentAttempt(request)) return;
		try {
			itemPriceResultService.apply(request, itemCashService.estimatePrice(request));
		} catch (PriceEstimationException exception) {
			retryScheduler.recordFailure(request, exception.failure());
		}
	}
}

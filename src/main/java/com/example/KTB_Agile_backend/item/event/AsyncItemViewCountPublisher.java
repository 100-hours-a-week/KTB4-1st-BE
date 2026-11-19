package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsyncItemViewCountPublisher {

	private static final int MAX_ATTEMPTS = 3;
	private static final long RETRY_DELAY_MILLIS = 100;

	private final ItemService itemService;

	@Async("viewCountExecutor")
	public void publish(ItemViewEvent event) {
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				itemService.recordView(event.userId(), event.itemId());
				return;
			} catch (TransientDataAccessException exception) {
				if (attempt == MAX_ATTEMPTS) {
					throw exception;
				}
				try {
					Thread.sleep(RETRY_DELAY_MILLIS);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					IllegalStateException retryInterrupted = new IllegalStateException(
							"Interrupted while retrying item view count", interrupted);
					retryInterrupted.addSuppressed(exception);
					throw retryInterrupted;
				}
			}
		}
	}
}

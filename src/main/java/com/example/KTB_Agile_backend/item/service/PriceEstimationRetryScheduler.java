package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.item.entity.ItemPriceStatus;
import com.example.KTB_Agile_backend.item.entity.PriceEstimationRetrySchedule;
import com.example.KTB_Agile_backend.item.event.ItemPriceEstimationFailed;
import com.example.KTB_Agile_backend.item.event.ItemPriceEstimationOutboxEvent;
import com.example.KTB_Agile_backend.item.event.ItemPriceEstimationOutboxEventType;
import com.example.KTB_Agile_backend.item.event.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.repository.ItemPriceEstimationOutboxEventRepository;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.PriceEstimationRetryScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PriceEstimationRetryScheduler {
	private final PriceEstimationRetryScheduleRepository scheduleRepository;
	private final ItemPriceEstimationOutboxEventRepository outboxRepository;
	private final ItemRepository itemRepository;
	private final PriceEstimationRetryPolicy retryPolicy;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public boolean isCurrentAttempt(ItemPriceEstimationRequested request) {
		if (!itemRepository.existsByIdAndPriceRequestIdAndPriceStatus(
				request.itemId(), request.requestId(), ItemPriceStatus.PENDING)) return false;
		return scheduleRepository.findById(request.requestId())
				.map(schedule -> schedule.isCurrentAttempt(request.attemptCount()))
				.orElse(request.attemptCount() == 1);
	}

	@Transactional
	public void recordFailure(ItemPriceEstimationRequested request, PriceEstimationRetryPolicy.Failure failure) {
		PriceEstimationRetrySchedule schedule = scheduleRepository.findByRequestIdForUpdate(request.requestId())
				.orElse(null);
		if (schedule == null && request.attemptCount() == 1) {
			// The Item row arbitrates first-attempt success and failure before a retry row exists.
			if (itemRepository.findPendingPriceRequestForUpdate(request.itemId(), request.requestId()).isEmpty()) return;
			schedule = scheduleRepository.findByRequestIdForUpdate(request.requestId()).orElse(null);
		}
		if (schedule == null ? request.attemptCount() != 1 : !schedule.isCurrentAttempt(request.attemptCount())) return;

		boolean validationRetryUsed = schedule != null && schedule.isValidationRetryUsed();
		if (retryPolicy.shouldRetry(request.attemptCount(), validationRetryUsed, failure)) {
			validationRetryUsed = validationRetryUsed
					|| failure == PriceEstimationRetryPolicy.Failure.RESPONSE_VALIDATION;
			int nextAttempt = request.attemptCount() + 1;
			LocalDateTime nextAttemptAt = LocalDateTime.now().plus(retryPolicy.delayAfterFailure(request.attemptCount()));
			if (schedule == null) {
				scheduleRepository.save(new PriceEstimationRetrySchedule(request.requestId(),
						request.originalRequestEventId(), nextAttempt, nextAttemptAt, validationRetryUsed));
			} else {
				schedule.schedule(nextAttempt, nextAttemptAt, validationRetryUsed);
			}
			return;
		}

		if (itemRepository.failPriceRequest(request.itemId(), request.requestId()) == 1) {
			String eventId = UUID.randomUUID().toString();
			ItemPriceEstimationFailed failed = ItemPriceEstimationFailed.from(request, failure.name(), LocalDateTime.now());
			outboxRepository.save(new ItemPriceEstimationOutboxEvent(eventId, request.requestId(),
					request.originalRequestEventId(), "PRICE_ESTIMATION_FAILED:" + request.requestId(),
					ItemPriceEstimationOutboxEventType.PRICE_ESTIMATION_FAILED, request.itemId(),
					request.attemptCount(), serialize(failed)));
		}
		if (schedule != null) scheduleRepository.delete(schedule);
	}

	@Scheduled(fixedDelayString = "${app.item-price-estimation.retry.poll-interval:500ms}")
	@Transactional
	public void publishDueRetries() {
		LocalDateTime now = LocalDateTime.now();
		for (String requestId : scheduleRepository.findDueRequestIds(now, PageRequest.of(0, 100))) {
			scheduleRepository.findByRequestIdForUpdate(requestId).ifPresent(schedule -> publishIfDue(schedule, now));
		}
	}

	private void publishIfDue(PriceEstimationRetrySchedule schedule, LocalDateTime now) {
		if (!schedule.isDueAt(now)) return;
		ItemPriceEstimationOutboxEvent originalEvent = outboxRepository
				.findByEventId(schedule.getOriginalRequestEventId()).orElseThrow();
		ItemPriceEstimationRequested originalRequest = deserializeRequest(originalEvent.getPayload());
		String eventId = UUID.randomUUID().toString();
		ItemPriceEstimationRequested retry = originalRequest.retry(eventId, schedule.getAttemptCount());
		schedule.markPublished();
		outboxRepository.save(new ItemPriceEstimationOutboxEvent(eventId, retry.requestId(),
				retry.originalRequestEventId(),
				"PRICE_ESTIMATION_REQUESTED:" + retry.requestId() + ":" + retry.attemptCount(),
				ItemPriceEstimationOutboxEventType.PRICE_ESTIMATION_REQUESTED, retry.itemId(),
				retry.attemptCount(), serialize(retry)));
	}

	private ItemPriceEstimationRequested deserializeRequest(String payload) {
		try {
			return objectMapper.readValue(payload, ItemPriceEstimationRequested.class);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Unable to read original item price estimation request", exception);
		}
	}

	private String serialize(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Unable to serialize item price estimation event", exception);
		}
	}
}

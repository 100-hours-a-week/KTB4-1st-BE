package com.example.KTB_Agile_backend.item.price.retry;

import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetrySchedule;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationFailed;
import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationOutboxEvent;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationOutboxEventType;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationOutboxEventRepository;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetryScheduleRepository;
import com.example.KTB_Agile_backend.item.price.config.PriceEstimationRetryProperties;
import com.example.KTB_Agile_backend.item.price.PriceEstimationFailure;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceEstimationRetrySchedulerTest {
	@Test
	void persistsNextAttemptAndTimeWithoutCreatingOutboxUntilItIsDue() {
		var schedules = mock(PriceEstimationRetryScheduleRepository.class);
		var outbox = mock(ItemPriceEstimationOutboxEventRepository.class);
		var items = mock(ItemRepository.class);
		when(schedules.findByRequestIdForUpdate("request-1")).thenReturn(Optional.empty(), Optional.empty());
		when(items.findPendingPriceRequestForUpdate(17L, "request-1"))
				.thenReturn(Optional.of(mock(Item.class)));
		var scheduler = scheduler(schedules, outbox, items, retryPolicy(4));
		var before = LocalDateTime.now();

		scheduler.recordFailure(request(1), PriceEstimationFailure.TRANSIENT);

		var schedule = org.mockito.ArgumentCaptor.forClass(PriceEstimationRetrySchedule.class);
		verify(schedules).save(schedule.capture());
		assertThat(schedule.getValue().getAttemptCount()).isEqualTo(2);
		assertThat(schedule.getValue().getNextAttemptAt()).isAfterOrEqualTo(before)
				.isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
		verify(outbox, never()).save(any());
	}

	@Test
	void terminalFailureChangesItemAndWritesDltIntent() throws Exception {
		var schedules = mock(PriceEstimationRetryScheduleRepository.class);
		var outbox = mock(ItemPriceEstimationOutboxEventRepository.class);
		var items = mock(ItemRepository.class);
		when(schedules.findByRequestIdForUpdate("request-1")).thenReturn(Optional.empty(), Optional.empty());
		when(items.findPendingPriceRequestForUpdate(17L, "request-1"))
				.thenReturn(Optional.of(mock(Item.class)));
		when(items.failPriceRequest(17L, "request-1")).thenReturn(1);
		var scheduler = scheduler(schedules, outbox, items, retryPolicy(1));

		scheduler.recordFailure(request(1), PriceEstimationFailure.CONFIGURATION);

		var event = org.mockito.ArgumentCaptor.forClass(ItemPriceEstimationOutboxEvent.class);
		verify(outbox).save(event.capture());
		assertThat(event.getValue().getEventType())
				.isEqualTo(ItemPriceEstimationOutboxEventType.PRICE_ESTIMATION_FAILED);
		var failed = new ObjectMapper().readValue(event.getValue().getPayload(), ItemPriceEstimationFailed.class);
		assertThat(failed.requestId()).isEqualTo("request-1");
		assertThat(failed.originalRequestEventId()).isEqualTo("event-1");
		assertThat(failed.failedAttemptEventId()).isEqualTo("attempt-1");
		assertThat(failed.attemptCount()).isEqualTo(1);
		assertThat(failed.failureClassification()).isEqualTo("CONFIGURATION");
		verify(schedules, never()).save(any());
	}

	private static PriceEstimationRetryScheduler scheduler(PriceEstimationRetryScheduleRepository schedules,
			ItemPriceEstimationOutboxEventRepository outbox, ItemRepository items, PriceEstimationRetryPolicy policy) {
		return new PriceEstimationRetryScheduler(schedules, outbox, items, policy, new ObjectMapper());
	}

	private static PriceEstimationRetryPolicy retryPolicy(int maxAttempts) {
		return new PriceEstimationRetryPolicy(new PriceEstimationRetryProperties(true, maxAttempts, true,
				Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), true, Duration.ofMillis(500)));
	}

	private static ItemPriceEstimationRequested request(int attempt) {
		return new ItemPriceEstimationRequested("attempt-" + attempt, "request-1", "event-1", 17L, "keyword",
				"title", "content", new BigDecimal("0.20"), new BigDecimal("0.40"), attempt);
	}
}

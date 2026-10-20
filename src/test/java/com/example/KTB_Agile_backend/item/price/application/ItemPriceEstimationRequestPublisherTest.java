package com.example.KTB_Agile_backend.item.price.application;

import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationOutboxEvent;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationOutboxEventType;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationOutboxEventRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ItemPriceEstimationRequestPublisherTest {
	@Test
	void persistsInitialOutboxEventWithoutRetrySchedule() throws Exception {
		var outbox = mock(ItemPriceEstimationOutboxEventRepository.class);
		var publisher = new ItemPriceEstimationRequestPublisher(outbox, new ObjectMapper());
		var item = new Item(mock(User.class), "title", "content", 1, ItemState.AVAILABLE,
				new BigDecimal("0.50"), new BigDecimal("0.30"));
		item.requestPrice("request-1");
		ReflectionTestUtils.setField(item, "id", 123L);

		publisher.request(item, " book ");

		var event = org.mockito.ArgumentCaptor.forClass(ItemPriceEstimationOutboxEvent.class);
		verify(outbox).save(event.capture());
		assertThat(event.getValue().getEventType())
				.isEqualTo(ItemPriceEstimationOutboxEventType.PRICE_ESTIMATION_REQUESTED);
		var request = new ObjectMapper().readValue(event.getValue().getPayload(), ItemPriceEstimationRequested.class);
		assertThat(request.itemId()).isEqualTo(123L);
		assertThat(request.keyword()).isEqualTo(" book ");
		assertThat(request.attemptCount()).isEqualTo(1);
		assertThat(request.eventId()).isEqualTo(request.originalRequestEventId());
	}
}

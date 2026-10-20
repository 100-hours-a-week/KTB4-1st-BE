package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.event.ItemPriceEstimationOutboxEvent;
import com.example.KTB_Agile_backend.item.event.ItemPriceEstimationOutboxEventType;
import com.example.KTB_Agile_backend.item.event.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.repository.ItemPriceEstimationOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemPriceEstimationRequestPublisher {
	private final ItemPriceEstimationOutboxEventRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void request(Item item, String normalizedKeyword) {
		String eventId = UUID.randomUUID().toString();
		ItemPriceEstimationRequested request = new ItemPriceEstimationRequested(
				eventId, item.getPriceRequestId(), eventId, item.getId(), normalizedKeyword,
				item.getTitle(), item.getContent(), item.getValueGapToleranceScore(),
				item.getExchangeUrgencyScore(), 1);
		outboxRepository.save(new ItemPriceEstimationOutboxEvent(eventId, item.getPriceRequestId(), eventId,
				"PRICE_ESTIMATION_REQUESTED:" + item.getPriceRequestId() + ":1",
				ItemPriceEstimationOutboxEventType.PRICE_ESTIMATION_REQUESTED, item.getId(), 1, serialize(request)));
	}

	private String serialize(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Unable to serialize item price estimation event", exception);
		}
	}
}

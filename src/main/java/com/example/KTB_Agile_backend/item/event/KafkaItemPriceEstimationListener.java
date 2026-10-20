package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.item.service.ItemPriceEstimationProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaItemPriceEstimationListener {
	private final ObjectMapper objectMapper;
	private final ItemPriceEstimationProcessor processor;

	@KafkaListener(topics = KafkaTopics.ITEM_PRICE_ESTIMATION_REQUESTS)
	public void consume(String payload) {
		try {
			processor.process(objectMapper.readValue(payload, ItemPriceEstimationRequested.class));
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize item price estimation request", exception);
		}
	}
}

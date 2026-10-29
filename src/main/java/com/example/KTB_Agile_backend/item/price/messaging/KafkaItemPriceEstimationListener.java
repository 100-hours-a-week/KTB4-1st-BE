package com.example.KTB_Agile_backend.item.price.messaging;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.item.price.application.ItemPriceEstimationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaItemPriceEstimationListener {
	private final ObjectMapper objectMapper;
	private final ItemPriceEstimationProcessor processor;

	@KafkaListener(topics = KafkaTopics.ITEM_PRICE_ESTIMATION_REQUESTS)
	public void consume(ConsumerRecord<String, String> record) {
		try {
			processor.process(objectMapper.readValue(record.value(), ItemPriceEstimationRequested.class));
		} catch (JacksonException exception) {
			if (log.isErrorEnabled()) {
				log.error("Failed to deserialize Kafka event: topic={}, partition={}, offset={}",
						record.topic(), record.partition(), record.offset(), exception);
			}
			throw new IllegalArgumentException("Unable to deserialize item price estimation request", exception);
		} catch (RuntimeException exception) {
			if (log.isErrorEnabled()) {
				log.error("Failed to consume Kafka event: topic={}, partition={}, offset={}",
						record.topic(), record.partition(), record.offset(), exception);
			}
			throw exception;
		}
	}
}

package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaItemViewEventListener {

	private final ObjectMapper objectMapper;
	private final ItemService itemService;

	@RetryableTopic(
			attempts = "${app.kafka.retry.item-view.attempts}",
			backOff = @BackOff(delayString = "${app.kafka.retry.item-view.backoff}"),
			autoStartDltHandler = "false"
	)
	@KafkaListener(topics = KafkaTopics.ITEM_VIEW_EVENTS)
	public void consume(ConsumerRecord<String, String> record) {
		try {
			ItemViewEvent event = fromJson(record.value());
			itemService.recordView(event.userId(), event.itemId());
		} catch (RuntimeException exception) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to consume Kafka event: topic={}, partition={}, offset={}",
						record.topic(), record.partition(), record.offset(), exception);
			}
			throw exception;
		}
	}

	private ItemViewEvent fromJson(String payload) {
		try {
			return objectMapper.readValue(payload, ItemViewEvent.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize item view event", exception);
		}
	}
}

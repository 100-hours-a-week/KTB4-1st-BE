package com.example.KTB_Agile_backend.search.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.search.service.SearchHistoryService;
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
public class KafkaSearchHistoryEventListener {

	private final ObjectMapper objectMapper;
	private final SearchHistoryService searchHistoryService;

	@RetryableTopic(
			attempts = "${app.kafka.retry.search-history.attempts}",
			backOff = @BackOff(
					delayString = "${app.kafka.retry.search-history.backoff}",
					multiplierString = "${app.kafka.retry.search-history.multiplier}",
					maxDelayString = "${app.kafka.retry.search-history.max-backoff}"
			),
			autoStartDltHandler = "false"
	)
	@KafkaListener(topics = KafkaTopics.SEARCH_HISTORY_EVENTS)
	public void consume(ConsumerRecord<String, String> record) {
		try {
			SearchHistoryEvent event = fromJson(record.value());
			searchHistoryService.recordProductSearch(event.userId(), event.keyword());
		} catch (RuntimeException exception) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to consume Kafka event: topic={}, partition={}, offset={}",
						record.topic(), record.partition(), record.offset(), exception);
			}
			throw exception;
		}
	}

	private SearchHistoryEvent fromJson(String payload) {
		try {
			return objectMapper.readValue(payload, SearchHistoryEvent.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize search history event", exception);
		}
	}
}

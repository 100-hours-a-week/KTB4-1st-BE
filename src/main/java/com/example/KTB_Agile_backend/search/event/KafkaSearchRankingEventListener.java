package com.example.KTB_Agile_backend.search.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.search.service.SearchRankingService;
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
public class KafkaSearchRankingEventListener {

	private static final String CONSUMER_GROUP = "search_ranking";

	private final ObjectMapper objectMapper;
	private final SearchRankingService searchRankingService;

	@KafkaListener(topics = KafkaTopics.SEARCH_HISTORY_EVENTS, groupId = CONSUMER_GROUP)
	public void consume(ConsumerRecord<String, String> record) {
		try {
			SearchHistoryEvent event = objectMapper.readValue(record.value(), SearchHistoryEvent.class);
			if (event == null || event.keyword() == null || event.keyword().isBlank()) {
				throw new IllegalArgumentException("Search ranking event keyword must not be blank");
			}
			long timestamp = record.timestamp() < 0 ? System.currentTimeMillis() : record.timestamp();
			searchRankingService.recordSearch(event.keyword(), timestamp, record.partition(), record.offset());
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize search ranking event", exception);
		} catch (RuntimeException exception) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to consume Kafka event: topic={}, partition={}, offset={}",
						record.topic(), record.partition(), record.offset(), exception);
			}
			throw exception;
		}
	}
}

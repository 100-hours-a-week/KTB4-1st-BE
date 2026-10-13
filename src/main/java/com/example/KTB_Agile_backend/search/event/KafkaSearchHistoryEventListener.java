package com.example.KTB_Agile_backend.search.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaSearchHistoryEventListener {

	private final ObjectMapper objectMapper;
	private final SearchHistoryService searchHistoryService;

	@KafkaListener(topics = KafkaTopics.SEARCH_HISTORY_EVENTS)
	public void consume(String payload) {
		SearchHistoryEvent event = fromJson(payload);
		searchHistoryService.recordProductSearch(event.userId(), event.keyword());
	}

	private SearchHistoryEvent fromJson(String payload) {
		try {
			return objectMapper.readValue(payload, SearchHistoryEvent.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize search history event", exception);
		}
	}
}

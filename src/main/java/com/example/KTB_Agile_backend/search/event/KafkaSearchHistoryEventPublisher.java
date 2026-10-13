package com.example.KTB_Agile_backend.search.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaSearchHistoryEventPublisher implements SearchHistoryEventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public void publish(SearchHistoryEvent event) {
		kafkaTemplate.send(KafkaTopics.SEARCH_HISTORY_EVENTS, event.userId().toString(), toJson(event))
				.whenComplete((result, error) -> {
					if (error != null) {
						log.error("Failed to publish search history event", error);
					}
				});
	}

	private String toJson(SearchHistoryEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Unable to serialize search history event", exception);
		}
	}
}

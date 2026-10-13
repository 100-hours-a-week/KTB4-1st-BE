package com.example.KTB_Agile_backend.item.event;

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
public class KafkaItemViewEventPublisher implements ItemViewEventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public void publish(ItemViewEvent event) {
		kafkaTemplate.send(KafkaTopics.ITEM_VIEW_EVENTS, event.itemId().toString(), toJson(event))
				.whenComplete((result, error) -> {
					if (error != null) {
						log.error("Failed to publish item view event", error);
					}
				});
	}

	private String toJson(ItemViewEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Unable to serialize item view event", exception);
		}
	}
}

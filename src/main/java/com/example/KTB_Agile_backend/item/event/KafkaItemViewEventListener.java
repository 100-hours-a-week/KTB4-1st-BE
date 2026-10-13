package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaItemViewEventListener {

	private final ObjectMapper objectMapper;
	private final ItemService itemService;

	@KafkaListener(topics = KafkaTopics.ITEM_VIEW_EVENTS)
	public void consume(String payload) {
		ItemViewEvent event = fromJson(payload);
		itemService.recordView(event.userId(), event.itemId());
	}

	private ItemViewEvent fromJson(String payload) {
		try {
			return objectMapper.readValue(payload, ItemViewEvent.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize item view event", exception);
		}
	}
}

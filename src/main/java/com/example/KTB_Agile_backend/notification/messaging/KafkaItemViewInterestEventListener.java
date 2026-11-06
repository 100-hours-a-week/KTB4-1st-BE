package com.example.KTB_Agile_backend.notification.messaging;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.item.event.ItemViewEvent;
import com.example.KTB_Agile_backend.notification.service.ItemViewInterestNotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaItemViewInterestEventListener {
	private final ObjectMapper objectMapper;
	private final ItemViewInterestNotificationService service;

	@KafkaListener(topics = KafkaTopics.ITEM_VIEW_EVENTS, groupId = "item_view_interest_notifications")
	public void consume(ConsumerRecord<String, String> record) {
		try {
			ItemViewEvent event = objectMapper.readValue(record.value(), ItemViewEvent.class);
			if (event.userId() == null || event.itemId() == null) throw new IllegalArgumentException("View event userId and itemId are required");
			service.recordView(event.userId(), event.itemId(), record.timestamp() < 0 ? System.currentTimeMillis() : record.timestamp(), record.partition(), record.offset());
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize item view event", exception);
		}
	}
}

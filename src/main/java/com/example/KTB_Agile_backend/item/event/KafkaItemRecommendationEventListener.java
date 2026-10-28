package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.item.service.ItemRecommendationService;
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
public class KafkaItemRecommendationEventListener {

	private static final String CONSUMER_GROUP = "item_recommend";

	private final ObjectMapper objectMapper;
	private final ItemRecommendationService itemRecommendationService;

	@KafkaListener(
			topics = KafkaTopics.ITEM_VIEW_EVENTS,
			groupId = CONSUMER_GROUP,
			properties = "auto.offset.reset=by_duration:PT6H"
	)
	public void consume(ConsumerRecord<String, String> record) {
		try {
			ItemViewEvent event = objectMapper.readValue(record.value(), ItemViewEvent.class);
			if (event.itemId() == null) {
				throw new IllegalArgumentException("Item view event itemId must not be null");
			}
			long timestamp = record.timestamp() < 0 ? System.currentTimeMillis() : record.timestamp();
			itemRecommendationService.recordView(
					event.itemId(), timestamp, record.partition(), record.offset());
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize item view event", exception);
		} catch (RuntimeException exception) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to consume recommendation event: topic={}, partition={}, offset={}",
						record.topic(), record.partition(), record.offset(), exception);
			}
			throw exception;
		}
	}
}

package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.common.config.KafkaTopics;
import com.example.KTB_Agile_backend.item.service.ItemCoViewRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaItemCoViewRecommendationEventListener {

	private static final String CONSUMER_GROUP = "item_co_view_recommendations";

	private final ObjectMapper objectMapper;
	private final ItemCoViewRecommendationService recommendationService;

	@KafkaListener(
			topics = KafkaTopics.ITEM_VIEW_EVENTS,
			groupId = CONSUMER_GROUP,
			properties = "auto.offset.reset=by_duration:PT168H"
	)
	public void consume(ConsumerRecord<String, String> record) {
		try {
			ItemViewEvent event = objectMapper.readValue(record.value(), ItemViewEvent.class);
			if (event.userId() == null || event.itemId() == null) {
				throw new IllegalArgumentException("Item view event userId and itemId must not be null");
			}
			long timestamp = record.timestamp() < 0 ? System.currentTimeMillis() : record.timestamp();
			LocalDateTime viewedAt = LocalDateTime.ofInstant(
					Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
			recommendationService.recordView(event.userId(), event.itemId(), viewedAt);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize item view event", exception);
		} catch (RuntimeException exception) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to consume co-view recommendation event: topic={}, partition={}, offset={}",
						record.topic(), record.partition(), record.offset(), exception);
			}
			throw exception;
		}
	}
}

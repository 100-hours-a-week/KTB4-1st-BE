package com.example.KTB_Agile_backend.common.config;

import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Conditional(KafkaAddressCondition.Configured.class)
public class KafkaTopicConfig {
	private static final String EVENT_RETENTION_MS = Long.toString(Duration.ofDays(7).toMillis());

	@Bean
	NewTopic itemViewEventsTopic() {
		return TopicBuilder.name(KafkaTopics.ITEM_VIEW_EVENTS)
				.partitions(1)
				.replicas(1)
				.config(TopicConfig.RETENTION_MS_CONFIG, EVENT_RETENTION_MS)
				.build();
	}

	@Bean
	NewTopic searchHistoryEventsTopic() {
		return TopicBuilder.name(KafkaTopics.SEARCH_HISTORY_EVENTS)
				.partitions(1)
				.replicas(1)
				.config(TopicConfig.RETENTION_MS_CONFIG, EVENT_RETENTION_MS)
				.build();
	}

	@Bean
	NewTopic itemPriceEstimationRequestsTopic() {
		return TopicBuilder.name(KafkaTopics.ITEM_PRICE_ESTIMATION_REQUESTS)
				.partitions(1).replicas(1).config(TopicConfig.RETENTION_MS_CONFIG, EVENT_RETENTION_MS).build();
	}

	@Bean
	NewTopic itemPriceEstimationFailedTopic() {
		return TopicBuilder.name(KafkaTopics.ITEM_PRICE_ESTIMATION_FAILED)
				.partitions(1).replicas(1).config(TopicConfig.RETENTION_MS_CONFIG, EVENT_RETENTION_MS).build();
	}
}

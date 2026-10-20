package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.common.config.KafkaTopics;

public enum ItemPriceEstimationOutboxEventType {
	PRICE_ESTIMATION_REQUESTED(KafkaTopics.ITEM_PRICE_ESTIMATION_REQUESTS),
	PRICE_ESTIMATION_FAILED(KafkaTopics.ITEM_PRICE_ESTIMATION_FAILED);

	private final String topic;

	ItemPriceEstimationOutboxEventType(String topic) {
		this.topic = topic;
	}

	public String topic() {
		return topic;
	}
}

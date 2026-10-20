package com.example.KTB_Agile_backend.common.config;

public final class KafkaTopics {

	public static final String ITEM_VIEW_EVENTS = "item-view-events";
	public static final String SEARCH_HISTORY_EVENTS = "search-history-events";
	public static final String ITEM_PRICE_ESTIMATION_REQUESTS = "item-price-estimation-requests";
	public static final String ITEM_PRICE_ESTIMATION_FAILED = "item-price-estimation-failed";

	private KafkaTopics() {
	}
}

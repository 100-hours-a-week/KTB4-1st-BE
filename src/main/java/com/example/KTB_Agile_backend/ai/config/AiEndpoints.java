package com.example.KTB_Agile_backend.ai.config;

public final class AiEndpoints {

	public static final String IMAGE_ANALYSIS = "/api/vision/analyze-item";
	public static final String TEXT_MODERATION = "/api/moderation/check-text";
	public static final String ITEM_PRICE = "/value/estimate-price";
	public static final String SMART_SEARCH = "/api/search/rank-candidates";
	public static final String CHATBOT_ASK = "/api/chatbot/ask";

	private AiEndpoints() {
	}
}

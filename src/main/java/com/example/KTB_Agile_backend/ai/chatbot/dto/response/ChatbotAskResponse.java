package com.example.KTB_Agile_backend.ai.chatbot.dto.response;

public record ChatbotAskResponse(String answer, Action action) {
	public record Action(String label, Screen screen) {
	}

	public enum Screen {
		MY_PAGE,
		ITEM_LIST,
		ITEM_ADD,
		CHAT_LIST,
		GROUP_MANAGE,
		INTEREST_LIST,
		INQUIRY;

		public static Screen fromValue(String value) {
			if (value == null) {
				return null;
			}
			try {
				return valueOf(value);
			} catch (IllegalArgumentException ignored) {
				return null;
			}
		}
	}
}

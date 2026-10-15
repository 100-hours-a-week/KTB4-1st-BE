package com.example.KTB_Agile_backend.ai.chatbot.dto.ai;

import java.util.List;

public record ChatbotAiRequest(String message, List<HistoryMessage> history) {
	public record HistoryMessage(String role, String content) {
	}
}

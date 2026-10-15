package com.example.KTB_Agile_backend.ai.chatbot.dto.ai;

public record ChatbotAiResponse(String answer, Action action) {
	public record Action(String label, String screen) {
	}
}

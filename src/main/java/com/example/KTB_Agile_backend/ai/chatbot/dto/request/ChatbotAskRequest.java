package com.example.KTB_Agile_backend.ai.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatbotAskRequest(
		@NotBlank(message = "메시지는 필수 입력값입니다.")
		String message
) {
}

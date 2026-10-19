package com.example.KTB_Agile_backend.ai.chatbot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatbotAskRequest(
		@NotBlank(message = "메시지는 필수 입력값입니다.")
		String message,
		@Size(max = 10, message = "대화 내역은 10개까지 전달할 수 있습니다.")
		List<@NotNull @Valid HistoryMessage> history
) {
	public ChatbotAskRequest {
		history = history == null ? List.of() : history;
	}

	public record HistoryMessage(
			@NotBlank
			@Pattern(regexp = "user|assistant")
			String role,
			@NotBlank
			String content
	) {
	}
}

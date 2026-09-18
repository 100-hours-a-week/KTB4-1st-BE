package com.example.KTB_Agile_backend.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserPreferenceResponse(
		Long userPreferenceId,
		List<Answer> answers,
		LocalDateTime createdAt
) {

	public record Answer(String question, String answer) {
	}
}

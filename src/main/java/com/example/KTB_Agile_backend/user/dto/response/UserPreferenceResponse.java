package com.example.KTB_Agile_backend.user.dto.response;

import com.example.KTB_Agile_backend.user.dto.UserPreferenceAnswerOption;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceQuestion;
import com.example.KTB_Agile_backend.user.entity.UserPreference;

import java.time.LocalDateTime;
import java.util.List;

public record UserPreferenceResponse(
		Long userPreferenceId,
		List<Answer> answers,
		LocalDateTime createdAt
) {

	public static UserPreferenceResponse from(UserPreference preference) {
		return new UserPreferenceResponse(
				preference.getId(),
				preference.getAnswers().stream()
						.map(answer -> new Answer(
								UserPreferenceQuestion.valueOf(answer.getQuestion()),
								UserPreferenceAnswerOption.valueOf(answer.getAnswer())
						))
						.toList(),
				preference.getCreatedAt()
		);
	}

	public record Answer(UserPreferenceQuestion question, UserPreferenceAnswerOption answer) {
	}
}

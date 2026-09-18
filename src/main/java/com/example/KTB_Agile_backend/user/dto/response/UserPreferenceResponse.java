package com.example.KTB_Agile_backend.user.dto.response;

import com.example.KTB_Agile_backend.user.entity.UserPreference;

import java.util.List;

public record UserPreferenceResponse(List<Answer> answers) {

	public static UserPreferenceResponse from(UserPreference preference) {
		return new UserPreferenceResponse(preference.getAnswers().stream()
				.map(answer -> new Answer(answer.getQuestion(), answer.getAnswer()))
				.toList());
	}

	public record Answer(String question, String answer) {
	}
}

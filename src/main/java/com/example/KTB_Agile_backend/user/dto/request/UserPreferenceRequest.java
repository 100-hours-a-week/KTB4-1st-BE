package com.example.KTB_Agile_backend.user.dto.request;

import com.example.KTB_Agile_backend.user.dto.UserPreferenceAnswerOption;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceQuestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserPreferenceRequest(
		@NotEmpty(message = "answers는 하나 이상이어야 합니다.")
		@Valid
		List<@NotNull(message = "answer 항목은 필수 입력값입니다.") Answer> answers
) {

	public record Answer(
			@NotNull(message = "question은 필수 입력값입니다.")
			UserPreferenceQuestion question,
			@NotNull(message = "answer는 필수 입력값입니다.")
			UserPreferenceAnswerOption answer
	) {
	}
}

package com.example.KTB_Agile_backend.user.dto.request;

import com.example.KTB_Agile_backend.user.dto.UserPreferenceAnswerOption;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceQuestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserPreferenceRequest(
		@NotNull(message = "질문 3개를 모두 입력해야 합니다.")
		@Size(min = 3, max = 3, message = "질문 3개를 모두 입력해야 합니다.")
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

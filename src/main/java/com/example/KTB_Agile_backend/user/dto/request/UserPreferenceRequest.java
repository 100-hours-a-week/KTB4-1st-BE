package com.example.KTB_Agile_backend.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserPreferenceRequest(
		@NotEmpty(message = "answers는 하나 이상이어야 합니다.")
		@Valid
		List<@NotNull(message = "answer 항목은 필수 입력값입니다.") Answer> answers
) {

	public record Answer(
			@NotBlank(message = "question은 필수 입력값입니다.")
			@Size(max = 1000, message = "question은 1000자 이하여야 합니다.")
			String question,
			@NotBlank(message = "answer는 필수 입력값입니다.")
			@Size(max = 1000, message = "answer는 1000자 이하여야 합니다.")
			String answer
	) {
	}
}

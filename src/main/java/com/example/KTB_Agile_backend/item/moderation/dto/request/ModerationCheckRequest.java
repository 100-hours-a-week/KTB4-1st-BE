package com.example.KTB_Agile_backend.item.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationCheckRequest(
		@NotBlank(message = "제목은 필수 입력값입니다.")
		@Size(max = 100, message = "제목은 100자 이내여야 합니다.")
		String title,

		@NotBlank(message = "내용은 필수 입력값입니다.")
		@Size(max = 2000, message = "내용은 2000자 이내여야 합니다.")
		String content
) {

	public ModerationCheckRequest {
		title = strip(title);
		content = strip(content);
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}

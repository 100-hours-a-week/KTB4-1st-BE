package com.example.KTB_Agile_backend.ai.messagerefine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MessageRefineRequest(
		@NotBlank(message = "메시지는 필수 입력값입니다.")
		String message
) {
}

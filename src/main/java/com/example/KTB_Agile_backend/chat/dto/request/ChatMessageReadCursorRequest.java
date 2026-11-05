package com.example.KTB_Agile_backend.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatMessageReadCursorRequest(
		@NotNull(message = "messageId는 필수 입력값입니다.")
		@Positive(message = "messageId는 양수여야 합니다.")
		Long messageId
) {
}

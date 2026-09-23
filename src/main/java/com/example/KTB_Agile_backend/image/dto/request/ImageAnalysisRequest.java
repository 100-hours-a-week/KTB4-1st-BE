package com.example.KTB_Agile_backend.image.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ImageAnalysisRequest(
		@NotBlank(message = "이미지 objectKey는 필수 입력값입니다.")
		String objectKey
) {
}

package com.example.KTB_Agile_backend.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImageAnalysisRequest(
		@NotEmpty(message = "이미지 objectKey는 1개 이상이어야 합니다.")
		@Size(max = 3, message = "이미지는 최대 3장까지 분석할 수 있습니다.")
		List<@NotBlank(message = "이미지 objectKey는 필수 입력값입니다.") String> objectKeys
) {
}

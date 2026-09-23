package com.example.KTB_Agile_backend.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignedUploadRequest(
		@NotBlank(message = "이미지 Content-Type은 필수 입력값입니다.")
		@Pattern(
				regexp = "image/(jpeg|png|webp)",
				message = "지원하지 않는 이미지 형식입니다."
		)
		String contentType
) {
}

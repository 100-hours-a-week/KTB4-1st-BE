package com.example.KTB_Agile_backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
		@NotBlank(message = "provider는 필수 입력값입니다.") String provider,
		@NotBlank(message = "유효하지 않은 인가 코드입니다.") String authorizationCode,
		@NotBlank(message = "state는 필수 입력값입니다.") String state
) {
}

package com.example.KTB_Agile_backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
		@NotBlank String provider,
		@NotBlank String authorizationCode,
		String state
) {
}

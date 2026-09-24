package com.example.KTB_Agile_backend.auth.dto.response;

public record TokenReissueResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		boolean needsPreferenceSetup
) {
}

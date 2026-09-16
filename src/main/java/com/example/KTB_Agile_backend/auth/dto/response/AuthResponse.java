package com.example.KTB_Agile_backend.auth.dto.response;

public record AuthResponse(
		String accessToken,
		String tokenType,
		Long expiresIn,
		boolean isNewUser,
		UserProfile user
) {
}

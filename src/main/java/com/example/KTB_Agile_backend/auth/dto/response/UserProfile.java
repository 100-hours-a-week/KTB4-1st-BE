package com.example.KTB_Agile_backend.auth.dto.response;

public record UserProfile(
		Long userId,
		String nickname,
		String profileImageUrl
) {
}

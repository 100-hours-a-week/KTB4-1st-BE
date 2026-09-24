package com.example.KTB_Agile_backend.auth.dto;

public record OAuthUserInfo(
		String provider,
		String providerUserId,
		String nickname,
		String profileImageUrl
) {
}

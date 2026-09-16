package com.example.KTB_Agile_backend.auth.dto.provider;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
		Long id,
		@JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

	public record KakaoAccount(Profile profile) {
	}

	public record Profile(
			String nickname,
			@JsonProperty("profile_image_url") String profileImageUrl
	) {
	}
}

package com.example.KTB_Agile_backend.auth.dto.provider;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse(
		@JsonProperty("access_token") String accessToken
) {
}

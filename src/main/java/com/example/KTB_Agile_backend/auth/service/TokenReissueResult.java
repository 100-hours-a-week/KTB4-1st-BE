package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.dto.response.TokenReissueResponse;

import java.time.Duration;

public record TokenReissueResult(
		TokenReissueResponse response,
		String refreshToken,
		Duration refreshTokenTtl
) {
}

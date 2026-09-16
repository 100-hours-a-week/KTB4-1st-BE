package com.example.KTB_Agile_backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class TokenService {

	private final Duration refreshTokenTtl;
	private final SecureRandom secureRandom = new SecureRandom();

	public TokenService(
			@Value("${auth.refresh-token-ttl-days}") long refreshTokenTtlDays
	) {
		this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
	}

	public String issueRefreshToken() {
		byte[] tokenBytes = new byte[32];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	public LocalDateTime refreshTokenExpiresAt() {
		return LocalDateTime.now().plus(refreshTokenTtl);
	}
}

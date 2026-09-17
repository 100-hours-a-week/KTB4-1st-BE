package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.user.entity.RefreshToken;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final Duration refreshTokenTtl;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			@Value("${auth.refresh-token-ttl-days}") long refreshTokenTtlDays
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
	}

	public String issue(User user) {
		byte[] tokenBytes = new byte[32];
		secureRandom.nextBytes(tokenBytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		refreshTokenRepository.save(new RefreshToken(
				user,
				Hashing.sha256(token),
				LocalDateTime.now().plus(refreshTokenTtl)
		));
		return token;
	}

	public User requireValidUser(String token) {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("refresh token must not be blank");
		}

		RefreshToken savedToken = refreshTokenRepository
				.findByTokenHashAndDeletedAtIsNull(Hashing.sha256(token))
				.orElseThrow(() -> new IllegalArgumentException("invalid refresh token"));
		if (!savedToken.getExpiresAt().isAfter(LocalDateTime.now())) {
			savedToken.revoke();
			throw new IllegalArgumentException("refresh token is expired");
		}
		return savedToken.getUser();
	}

	public void revoke(String token) {
		if (token == null || token.isBlank()) {
			return;
		}

		refreshTokenRepository
				.findByTokenHashAndDeletedAtIsNull(Hashing.sha256(token))
				.ifPresent(savedToken -> {
					savedToken.revoke();
					refreshTokenRepository.save(savedToken);
				});
	}
}

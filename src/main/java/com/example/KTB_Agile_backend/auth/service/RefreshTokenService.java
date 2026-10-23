package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.exception.AuthErrorCode;
import com.example.KTB_Agile_backend.auth.token.RefreshTokenStore;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.util.Hashing;
import com.example.KTB_Agile_backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

	private static final int TOKEN_SECRET_BYTES = 32;

	private final RefreshTokenStore refreshTokenStore;
	private final Duration refreshTokenTtl;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
			RefreshTokenStore refreshTokenStore,
			@Value("${auth.refresh-token-ttl-days}") long refreshTokenTtlDays
	) {
		this.refreshTokenStore = refreshTokenStore;
		this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
	}

	public String issue(User user) {
		String sessionId = UUID.randomUUID().toString();
		String token = createToken(sessionId);
		refreshTokenStore.create(sessionId, user.getId(), Hashing.sha256(token), refreshTokenTtl);
		return token;
	}

	public Long requireCurrentUserId(String token) {
		String sessionId = sessionId(token).orElseThrow(RefreshTokenService::invalidRefreshToken);
		return refreshTokenStore.findCurrentUserId(sessionId, Hashing.sha256(token))
				.orElseThrow(RefreshTokenService::invalidRefreshToken);
	}

	public RefreshTokenRotation rotate(String token) {
		String sessionId = sessionId(token).orElseThrow(RefreshTokenService::invalidRefreshToken);
		String replacement = createToken(sessionId);
		RefreshTokenStore.Rotation rotation = refreshTokenStore.rotate(
				sessionId,
				Hashing.sha256(token),
				Hashing.sha256(replacement)
		);
		if (rotation.status() != RefreshTokenStore.RotationStatus.ROTATED) {
			throw invalidRefreshToken();
		}
		return new RefreshTokenRotation(replacement, rotation.userId(), rotation.remainingTtl());
	}

	public void revoke(String token) {
		sessionId(token).ifPresent(sessionId ->
				refreshTokenStore.revoke(sessionId, Hashing.sha256(token))
		);
	}

	private String createToken(String sessionId) {
		byte[] tokenBytes = new byte[TOKEN_SECRET_BYTES];
		secureRandom.nextBytes(tokenBytes);
		return sessionId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	private static Optional<String> sessionId(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		int separator = token.indexOf('.');
		if (separator < 1 || separator == token.length() - 1 || token.indexOf('.', separator + 1) >= 0) {
			return Optional.empty();
		}
		try {
			String sessionId = UUID.fromString(token.substring(0, separator)).toString();
			if (Base64.getUrlDecoder().decode(token.substring(separator + 1)).length != TOKEN_SECRET_BYTES) {
				return Optional.empty();
			}
			return Optional.of(sessionId);
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	private static ApiException invalidRefreshToken() {
		return new ApiException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
	}
}

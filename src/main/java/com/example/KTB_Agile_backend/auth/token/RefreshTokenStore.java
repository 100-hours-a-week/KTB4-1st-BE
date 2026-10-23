package com.example.KTB_Agile_backend.auth.token;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {
	void create(String sessionId, Long userId, String tokenHash, Duration ttl);

	Optional<Long> findCurrentUserId(String sessionId, String tokenHash);

	Rotation rotate(String sessionId, String oldTokenHash, String newTokenHash);

	void revoke(String sessionId, String tokenHash);

	enum RotationStatus {
		ROTATED,
		INVALID
	}

	record Rotation(RotationStatus status, Long userId, Duration remainingTtl) {
	}
}

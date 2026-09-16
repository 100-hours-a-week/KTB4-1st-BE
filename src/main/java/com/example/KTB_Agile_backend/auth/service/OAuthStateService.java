package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.state.OAuthStateStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class OAuthStateService {

	private static final Duration STATE_TTL = Duration.ofMinutes(5);

	private final OAuthStateStore oauthStateStore;
	private final SecureRandom secureRandom = new SecureRandom();

	public OAuthStateService(OAuthStateStore oauthStateStore) {
		this.oauthStateStore = oauthStateStore;
	}

	@Transactional
	public String issue(String provider) {
		String normalizedProvider = normalizeProvider(provider);
		byte[] stateBytes = new byte[32];
		secureRandom.nextBytes(stateBytes);
		String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
		oauthStateStore.save(
				Hashing.sha256(state),
				normalizedProvider,
				Instant.now().plus(STATE_TTL)
		);
		return state;
	}

	@Transactional
	public void consume(String stateCookie, String requestState, String provider) {
		if (stateCookie == null || requestState == null
				|| !MessageDigest.isEqual(
						stateCookie.getBytes(StandardCharsets.UTF_8),
						requestState.getBytes(StandardCharsets.UTF_8))) {
			throw new IllegalArgumentException("invalid OAuth state");
		}

		String normalizedProvider = normalizeProvider(provider);
		Instant now = Instant.now();
		boolean consumed = oauthStateStore.consumeIfValid(
				Hashing.sha256(stateCookie),
				normalizedProvider,
				now
		);
		if (!consumed) {
			throw new IllegalArgumentException("expired or already consumed OAuth state");
		}
	}

	private static String normalizeProvider(String provider) {
		if (provider == null || provider.isBlank()) {
			throw new IllegalArgumentException("provider must not be blank");
		}
		return provider.trim().toUpperCase(Locale.ROOT);
	}
}

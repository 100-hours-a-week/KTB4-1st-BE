package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.state.RedisOAuthStateStore;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorDetail;
import com.example.KTB_Agile_backend.auth.exception.AuthErrorCode;
import com.example.KTB_Agile_backend.common.util.Hashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class OAuthStateService {

	private static final List<ErrorDetail> AUTHENTICATION_DETAILS = List.of(
			new ErrorDetail("state", "OAuth 인증 상태가 유효하지 않습니다.")
	);

	private final RedisOAuthStateStore oauthStateStore;
	private final Duration stateTtl;
	private final SecureRandom secureRandom = new SecureRandom();

	public OAuthStateService(
			RedisOAuthStateStore oauthStateStore,
			@Value("${auth.oauth.state-ttl-seconds}") long stateTtlSeconds
	) {
		this.oauthStateStore = oauthStateStore;
		this.stateTtl = Duration.ofSeconds(stateTtlSeconds);
	}

	public String issue(String provider) {
		String normalizedProvider = normalizeProvider(provider);
		byte[] stateBytes = new byte[32];
		secureRandom.nextBytes(stateBytes);
		String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
		oauthStateStore.save(
				Hashing.sha256(state),
				normalizedProvider,
				stateTtl
		);
		return state;
	}

	public void consume(String stateCookie, String requestState, String provider) {
		if (stateCookie == null || requestState == null
				|| !MessageDigest.isEqual(
						stateCookie.getBytes(StandardCharsets.UTF_8),
						requestState.getBytes(StandardCharsets.UTF_8))) {
			throw authenticationFailed();
		}

		String normalizedProvider = normalizeProvider(provider);
		boolean consumed = oauthStateStore.consumeIfValid(
				Hashing.sha256(stateCookie),
				normalizedProvider
		);
		if (!consumed) {
			throw authenticationFailed();
		}
	}

	private static String normalizeProvider(String provider) {
		if (provider == null || provider.isBlank()) {
			throw new ApiException(
					AuthErrorCode.AUTH_PROVIDER_REQUIRED,
					List.of(new ErrorDetail("provider", "provider는 필수 입력값입니다."))
			);
		}
		return provider.trim().toUpperCase(Locale.ROOT);
	}

	private static ApiException authenticationFailed() {
		return new ApiException(AuthErrorCode.AUTH_OAUTH_STATE_INVALID, AUTHENTICATION_DETAILS);
	}
}

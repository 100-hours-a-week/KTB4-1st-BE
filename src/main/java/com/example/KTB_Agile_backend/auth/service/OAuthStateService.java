package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.state.OAuthStateStore;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class OAuthStateService {

	private static final List<ErrorResponse.Field> AUTHENTICATION_DETAILS = List.of(
			new ErrorResponse.Field("authorizationCode", "만료되었거나 이미 사용된 인가 코드입니다.")
	);

	private final OAuthStateStore oauthStateStore;
	private final Duration stateTtl;
	private final SecureRandom secureRandom = new SecureRandom();

	public OAuthStateService(
			OAuthStateStore oauthStateStore,
			@Value("${auth.oauth.state-ttl-seconds}") long stateTtlSeconds
	) {
		this.oauthStateStore = oauthStateStore;
		this.stateTtl = Duration.ofSeconds(stateTtlSeconds);
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
				LocalDateTime.now().plus(stateTtl)
		);
		return state;
	}

	@Transactional
	public void consume(String stateCookie, String requestState, String provider) {
		if (stateCookie == null || requestState == null
				|| !MessageDigest.isEqual(
						stateCookie.getBytes(StandardCharsets.UTF_8),
						requestState.getBytes(StandardCharsets.UTF_8))) {
			throw authenticationFailed();
		}

		String normalizedProvider = normalizeProvider(provider);
		LocalDateTime now = LocalDateTime.now();
		boolean consumed = oauthStateStore.consumeIfValid(
				Hashing.sha256(stateCookie),
				normalizedProvider,
				now
		);
		if (!consumed) {
			throw authenticationFailed();
		}
	}

	private static String normalizeProvider(String provider) {
		if (provider == null || provider.isBlank()) {
			throw new ApiException(
					ErrorCode.BAD_REQUEST,
					"요청 값이 올바르지 않습니다.",
					List.of(new ErrorResponse.Field("provider", "provider는 필수 입력값입니다."))
			);
		}
		return provider.trim().toUpperCase(Locale.ROOT);
	}

	private static ApiException authenticationFailed() {
		return new ApiException(
				ErrorCode.UNAUTHORIZED,
				"인증에 실패했습니다. 인가 코드가 만료되었거나 유효하지 않습니다.",
				AUTHENTICATION_DETAILS
		);
	}
}

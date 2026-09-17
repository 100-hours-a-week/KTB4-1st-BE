package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.client.OAuthProviderClient;
import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.auth.dto.request.OAuthLoginRequest;
import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;
import com.example.KTB_Agile_backend.auth.dto.response.TokenReissueResponse;
import com.example.KTB_Agile_backend.auth.dto.response.UserProfile;
import com.example.KTB_Agile_backend.auth.token.AccessTokenIssuer;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import com.example.KTB_Agile_backend.user.service.AccountProvisioningService;
import com.example.KTB_Agile_backend.user.service.AccountResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String DEFAULT_PROVIDER = "KAKAO";
	private static final String TOKEN_TYPE = "Bearer";

	private final OAuthStateService oauthStateService;
	private final List<OAuthProviderClient> oauthProviderClients;
	private final AccountProvisioningService accountProvisioningService;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshTokenService refreshTokenService;

	public String issueOAuthState() {
		return oauthStateService.issue(DEFAULT_PROVIDER);
	}

	/**
	 * Controller가 refresh token을 HttpOnly 쿠키로 옮길 때 사용하는 로그인 결과다.
	 */
	@Transactional
	public AuthTokenResult oauthLoginWithTokens(OAuthLoginRequest request, String stateCookie) {
		String provider = normalizeProvider(request.provider());
		oauthStateService.consume(stateCookie, request.state(), provider);

		OAuthUserInfo userInfo = findProviderClient(provider).getUserInfo(request.authorizationCode());
		validateUserInfo(userInfo, provider);

		AccountResult accountResult = accountProvisioningService.findOrCreate(userInfo);
		User user = accountResult.user();
		boolean newUser = accountResult.newUser();
		ensureActive(user);
		String accessToken = accessTokenIssuer.issue(user);
		String refreshToken = refreshTokenService.issue(user);

		AuthResponse response = new AuthResponse(
				accessToken,
				TOKEN_TYPE,
				accessTokenIssuer.expiresInSeconds(),
				newUser,
				new UserProfile(user.getId(), user.getNickname(), user.getProfileImageUrl())
		);
		return new AuthTokenResult(response, refreshToken);
	}

	@Transactional
	public TokenReissueResponse reissueToken(String refreshToken) {
		User user = refreshTokenService.requireValidUser(refreshToken);
		ensureActive(user);
		return new TokenReissueResponse(
				accessTokenIssuer.issue(user),
				TOKEN_TYPE,
				accessTokenIssuer.expiresInSeconds()
		);
	}

	@Transactional
	public void logout(String refreshToken) {
		refreshTokenService.revoke(refreshToken);
	}

	private OAuthProviderClient findProviderClient(String provider) {
		return oauthProviderClients.stream()
				.filter(client -> client.provider().equals(provider))
				.findFirst()
				.orElseThrow(() -> new ApiException(
						ErrorCode.BAD_REQUEST,
						"요청 값이 올바르지 않습니다.",
						List.of(new ErrorResponse.Field("provider", "지원하지 않는 OAuth provider입니다."))
				));
	}

	private static void validateUserInfo(OAuthUserInfo userInfo, String provider) {
		if (userInfo == null
				|| !provider.equals(normalizeProvider(userInfo.provider()))
				|| userInfo.providerUserId() == null
				|| userInfo.providerUserId().isBlank()
				|| userInfo.nickname() == null
				|| userInfo.nickname().isBlank()) {
			throw authenticationFailed();
		}
	}

	private static void ensureActive(User user) {
		if (user.getUserStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
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
				List.of(new ErrorResponse.Field("authorizationCode", "만료되었거나 이미 사용된 인가 코드입니다."))
		);
	}
}

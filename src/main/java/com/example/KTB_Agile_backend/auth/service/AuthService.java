package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.client.OAuthProviderClient;
import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.auth.dto.request.OAuthLoginRequest;
import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;
import com.example.KTB_Agile_backend.auth.dto.response.TokenReissueResponse;
import com.example.KTB_Agile_backend.auth.dto.response.UserProfile;
import com.example.KTB_Agile_backend.auth.token.AccessTokenIssuer;
import com.example.KTB_Agile_backend.user.entity.RefreshToken;
import com.example.KTB_Agile_backend.user.entity.SocialAccount;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import com.example.KTB_Agile_backend.user.repository.RefreshTokenRepository;
import com.example.KTB_Agile_backend.user.repository.SocialAccountRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

	private static final String DEFAULT_PROVIDER = "KAKAO";
	private static final String TOKEN_TYPE = "Bearer";

	private final OAuthStateService oauthStateService;
	private final List<OAuthProviderClient> oauthProviderClients;
	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AccessTokenIssuer accessTokenIssuer;
	private final TokenService tokenService;

	public AuthService(
			OAuthStateService oauthStateService,
			List<OAuthProviderClient> oauthProviderClients,
			UserRepository userRepository,
			SocialAccountRepository socialAccountRepository,
			RefreshTokenRepository refreshTokenRepository,
			AccessTokenIssuer accessTokenIssuer,
			TokenService tokenService
	) {
		this.oauthStateService = oauthStateService;
		this.oauthProviderClients = oauthProviderClients;
		this.userRepository = userRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.accessTokenIssuer = accessTokenIssuer;
		this.tokenService = tokenService;
	}

	public String issueOAuthState() {
		return oauthStateService.issue(DEFAULT_PROVIDER);
	}

	@Transactional
	public AuthResponse oauthLogin(OAuthLoginRequest request, String stateCookie) {
		return oauthLoginWithTokens(request, stateCookie).response();
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

		SocialAccount socialAccount = socialAccountRepository
				.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
				.orElse(null);
		boolean newUser = socialAccount == null;
		User user;

		if (newUser) {
			user = userRepository.save(new User(userInfo.nickname()));
			socialAccount = socialAccountRepository.save(
					new SocialAccount(user, provider, userInfo.providerUserId())
			);
		} else {
			user = socialAccount.getUser();
			ensureActive(user);
			socialAccount.recordLogin();
			socialAccountRepository.save(socialAccount);
		}

		ensureActive(user);
		String accessToken = accessTokenIssuer.issueAccessToken(user);
		String refreshToken = tokenService.issueRefreshToken();
		refreshTokenRepository.save(new RefreshToken(
				user,
				Hashing.sha256(refreshToken),
				tokenService.refreshTokenExpiresAt()
		));

		AuthResponse response = new AuthResponse(
				accessToken,
				TOKEN_TYPE,
				accessTokenIssuer.accessTokenExpiresInSeconds(),
				newUser,
				new UserProfile(user.getId(), user.getNickname(), user.getProfileImageUrl())
		);
		return new AuthTokenResult(response, refreshToken);
	}

	@Transactional
	public TokenReissueResponse reissueToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new IllegalArgumentException("refresh token must not be blank");
		}

		RefreshToken savedToken = refreshTokenRepository
				.findByTokenHashAndDeletedAtIsNull(Hashing.sha256(refreshToken))
				.orElseThrow(() -> new IllegalArgumentException("invalid refresh token"));
		LocalDateTime now = LocalDateTime.now();
		if (!savedToken.getExpiresAt().isAfter(now)) {
			savedToken.revoke();
			throw new IllegalArgumentException("refresh token is expired");
		}

		User user = savedToken.getUser();
		ensureActive(user);
		return new TokenReissueResponse(
				accessTokenIssuer.issueAccessToken(user),
				TOKEN_TYPE,
				accessTokenIssuer.accessTokenExpiresInSeconds()
		);
	}

	@Transactional
	public void logout(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}

		refreshTokenRepository
				.findByTokenHashAndDeletedAtIsNull(Hashing.sha256(refreshToken))
				.ifPresent(token -> {
					token.revoke();
					refreshTokenRepository.save(token);
				});
	}

	private OAuthProviderClient findProviderClient(String provider) {
		return oauthProviderClients.stream()
				.filter(client -> client.provider().equals(provider))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("unsupported OAuth provider: " + provider));
	}

	private static void validateUserInfo(OAuthUserInfo userInfo, String provider) {
		if (userInfo == null
				|| !provider.equals(normalizeProvider(userInfo.provider()))
				|| userInfo.providerUserId() == null
				|| userInfo.providerUserId().isBlank()
				|| userInfo.nickname() == null
				|| userInfo.nickname().isBlank()) {
			throw new IllegalArgumentException("invalid OAuth user information");
		}
	}

	private static void ensureActive(User user) {
		if (user.getUserStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
			throw new IllegalStateException("user is not active");
		}
	}

	private static String normalizeProvider(String provider) {
		if (provider == null || provider.isBlank()) {
			throw new IllegalArgumentException("provider must not be blank");
		}
		return provider.trim().toUpperCase(Locale.ROOT);
	}
}

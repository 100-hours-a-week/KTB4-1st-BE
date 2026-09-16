package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.client.OAuthProviderClient;
import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.auth.dto.request.OAuthLoginRequest;
import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;
import com.example.KTB_Agile_backend.auth.dto.response.TokenReissueResponse;
import com.example.KTB_Agile_backend.auth.token.AccessTokenIssuer;
import com.example.KTB_Agile_backend.auth.token.RefreshTokenService;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.SocialAccountRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Test
	void createsUserAndIssuesBothTokensOnFirstLogin() {
		OAuthStateService stateService = mock(OAuthStateService.class);
		OAuthProviderClient providerClient = mock(OAuthProviderClient.class);
		UserRepository userRepository = mock(UserRepository.class);
		SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
		AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
		RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
		AuthService authService = new AuthService(
				stateService,
				List.of(providerClient),
				userRepository,
				socialAccountRepository,
				accessTokenIssuer,
				refreshTokenService
		);

		when(providerClient.provider()).thenReturn("KAKAO");
		when(providerClient.getUserInfo("authorization-code"))
				.thenReturn(new OAuthUserInfo("KAKAO", "provider-user-1", "kim", null));
		when(socialAccountRepository.findByProviderAndProviderUserId("KAKAO", "provider-user-1"))
				.thenReturn(Optional.empty());
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(socialAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(accessTokenIssuer.issue(any())).thenReturn("access-token");
		when(refreshTokenService.issue(any())).thenReturn("refresh-token");
		when(accessTokenIssuer.expiresInSeconds()).thenReturn(900L);

		AuthResponse response = authService.oauthLogin(
				new OAuthLoginRequest("kakao", "authorization-code", "state"),
				"state"
		);

		assertEquals("access-token", response.accessToken());
		assertTrue(response.isNewUser());
		verify(stateService).consume("state", "state", "KAKAO");
		verify(refreshTokenService).issue(any(User.class));
	}

	@Test
	void reissuesAccessTokenWithValidatedRefreshToken() {
		AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
		RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
		AuthService authService = new AuthService(
				mock(OAuthStateService.class),
				List.of(),
				mock(UserRepository.class),
				mock(SocialAccountRepository.class),
				accessTokenIssuer,
				refreshTokenService
		);
		User user = new User("kim");
		when(refreshTokenService.requireValidUser("refresh-token")).thenReturn(user);
		when(accessTokenIssuer.issue(user)).thenReturn("new-access-token");
		when(accessTokenIssuer.expiresInSeconds()).thenReturn(900L);

		TokenReissueResponse response = authService.reissueToken("refresh-token");

		assertEquals("new-access-token", response.accessToken());
		assertEquals("Bearer", response.tokenType());
		assertEquals(900L, response.expiresIn());
	}

	@Test
	void delegatesRefreshTokenRevocationOnLogout() {
		RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
		AuthService authService = new AuthService(
				mock(OAuthStateService.class),
				List.of(),
				mock(UserRepository.class),
				mock(SocialAccountRepository.class),
				mock(AccessTokenIssuer.class),
				refreshTokenService
		);

		authService.logout("refresh-token");

		verify(refreshTokenService).revoke("refresh-token");
	}
}

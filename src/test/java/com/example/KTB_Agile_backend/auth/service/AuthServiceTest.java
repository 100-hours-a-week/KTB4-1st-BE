package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.client.OAuthProviderClient;
import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.auth.dto.request.OAuthLoginRequest;
import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;
import com.example.KTB_Agile_backend.auth.token.AccessTokenIssuer;
import com.example.KTB_Agile_backend.user.entity.RefreshToken;
import com.example.KTB_Agile_backend.user.repository.RefreshTokenRepository;
import com.example.KTB_Agile_backend.user.repository.SocialAccountRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Test
	void createsUserAndStoresHashedRefreshTokenOnFirstLogin() {
		OAuthStateService stateService = mock(OAuthStateService.class);
		OAuthProviderClient providerClient = mock(OAuthProviderClient.class);
		UserRepository userRepository = mock(UserRepository.class);
		SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
		RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
		AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
		TokenService tokenService = mock(TokenService.class);
		AuthService authService = new AuthService(
				stateService,
				List.of(providerClient),
				userRepository,
				socialAccountRepository,
				refreshTokenRepository,
				accessTokenIssuer,
				tokenService
		);

		when(providerClient.provider()).thenReturn("KAKAO");
		when(providerClient.getUserInfo("authorization-code"))
				.thenReturn(new OAuthUserInfo("KAKAO", "provider-user-1", "kim", null));
		when(socialAccountRepository.findByProviderAndProviderUserId("KAKAO", "provider-user-1"))
				.thenReturn(Optional.empty());
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(socialAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(accessTokenIssuer.issueAccessToken(any())).thenReturn("access-token");
		when(tokenService.issueRefreshToken()).thenReturn("refresh-token");
		when(tokenService.refreshTokenExpiresAt()).thenReturn(LocalDateTime.now().plusDays(14));
		when(accessTokenIssuer.accessTokenExpiresInSeconds()).thenReturn(900L);

		AuthResponse response = authService.oauthLogin(
				new OAuthLoginRequest("kakao", "authorization-code", "state"),
				"state"
		);

		assertEquals("access-token", response.accessToken());
		assertTrue(response.isNewUser());
		verify(stateService).consume("state", "state", "KAKAO");
		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		assertEquals(Hashing.sha256("refresh-token"), refreshTokenCaptor.getValue().getTokenHash());
	}
}

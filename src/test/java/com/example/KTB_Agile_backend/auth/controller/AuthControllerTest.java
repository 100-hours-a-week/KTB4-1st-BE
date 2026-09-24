package com.example.KTB_Agile_backend.auth.controller;

import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;
import com.example.KTB_Agile_backend.auth.dto.response.TokenReissueResponse;
import com.example.KTB_Agile_backend.auth.dto.response.UserProfile;
import com.example.KTB_Agile_backend.auth.service.AuthService;
import com.example.KTB_Agile_backend.auth.service.AuthTokenResult;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

	private AuthService authService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		authService = mock(AuthService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new AuthController(authService, 14, 300, false, "http://127.0.0.1:3000"))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void issuesStateAndStoresItInAnHttpOnlyCookie() throws Exception {
		when(authService.issueOAuthState()).thenReturn("state-value");

		mockMvc.perform(get("/auth/oauth/state"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.state").value("state-value"))
				.andExpect(jsonPath("$.data.expiresIn").value(300))
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						containsString("oauth_state=state-value")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("Secure"))));
	}

	@Test
	void logsInAndReturnsAccessTokenWithRefreshCookie() throws Exception {
		AuthResponse authResponse = new AuthResponse(
				"access-token",
				"Bearer",
				900,
				true,
				new UserProfile(1L, "kim", null)
		);
		when(authService.oauthLoginWithTokens(any(), eq("state-value")))
				.thenReturn(new AuthTokenResult(authResponse, "refresh-token"));

		mockMvc.perform(post("/auth/oauth")
					.cookie(new jakarta.servlet.http.Cookie("oauth_state", "state-value"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "provider": "KAKAO",
							  "authorizationCode": "authorization-code",
							  "state": "state-value"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.user.userId").value(1))
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						containsString("refresh_token=refresh-token")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=1209600")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("Secure"))));

		verify(authService).oauthLoginWithTokens(any(), eq("state-value"));
	}

	@Test
	void handlesKakaoCallbackAndRedirectsToFrontendWithRefreshCookie() throws Exception {
		AuthResponse authResponse = new AuthResponse(
				"access-token",
				"Bearer",
				900,
				false,
				new UserProfile(1L, "kim", null)
		);
		when(authService.oauthLoginWithTokens(any(), eq("state-value")))
				.thenReturn(new AuthTokenResult(authResponse, "refresh-token"));

		mockMvc.perform(get("/auth/kakao/callback")
					.queryParam("code", "authorization-code")
					.queryParam("state", "state-value")
					.cookie(new jakarta.servlet.http.Cookie("oauth_state", "state-value")))
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, "http://127.0.0.1:3000"))
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						containsString("refresh_token=refresh-token")))
				.andExpect(content().string(""));

		verify(authService).oauthLoginWithTokens(
				argThat(request -> request.provider().equals("KAKAO")
						&& request.authorizationCode().equals("authorization-code")
						&& request.state().equals("state-value")),
				eq("state-value")
		);
	}

	@Test
	void logoutRevokesRefreshTokenAndDeletesCookie() throws Exception {
		mockMvc.perform(post("/auth/logout")
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token")))
				.andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						containsString("refresh_token=;")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("Secure"))));

		verify(authService).logout("refresh-token");
	}

	@Test
	void returnsStandardBadRequestForInvalidAuthorizationCode() throws Exception {
		mockMvc.perform(post("/auth/oauth")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "provider": "KAKAO",
							  "authorizationCode": "",
							  "state": "state-value"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("REQUEST_VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.message").value("요청 값이 올바르지 않습니다."))
				.andExpect(jsonPath("$.error.details[0].field").value("authorizationCode"))
				.andExpect(jsonPath("$.error.details[0].reason").value("유효하지 않은 인가 코드입니다."));
	}

	@Test
	void returnsStandardUnauthorizedForInvalidRefreshToken() throws Exception {
		when(authService.reissueToken("invalid-token"))
				.thenThrow(new ApiException(
						ErrorCode.AUTH_REFRESH_TOKEN_INVALID
				));

		mockMvc.perform(post("/auth/refresh")
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", "invalid-token")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_INVALID"))
				.andExpect(jsonPath("$.error.message").value("Refresh Token이 만료되었거나 유효하지 않습니다."))
				.andExpect(jsonPath("$.error.details").isEmpty());
	}

	@Test
	void returnsPreferenceSetupStatusWhenRefreshingAccessToken() throws Exception {
		when(authService.reissueToken("refresh-token"))
				.thenReturn(new TokenReissueResponse("access-token", "Bearer", 900, true));

		mockMvc.perform(post("/auth/refresh")
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.needsPreferenceSetup").value(true))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void hidesUnexpectedRefreshErrorBehindStandardResponse() throws Exception {
		when(authService.reissueToken("refresh-token"))
				.thenThrow(new RuntimeException("database failure"));

		mockMvc.perform(post("/auth/refresh")
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token")))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."))
				.andExpect(jsonPath("$.error.details").isEmpty());
	}
}

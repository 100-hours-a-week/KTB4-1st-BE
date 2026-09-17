package com.example.KTB_Agile_backend.auth.controller;

import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;
import com.example.KTB_Agile_backend.auth.dto.response.UserProfile;
import com.example.KTB_Agile_backend.auth.service.AuthService;
import com.example.KTB_Agile_backend.auth.service.AuthTokenResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

	private AuthService authService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		 authService = mock(AuthService.class);
		 mockMvc = MockMvcBuilders
				.standaloneSetup(new AuthController(authService, 14, 300))
				.build();
	}

	@Test
	void issuesStateAndStoresItInAnHttpOnlyCookie() throws Exception {
		when(authService.issueOAuthState()).thenReturn("state-value");

		mockMvc.perform(get("/api/auth/oauth/state"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.state").value("state-value"))
				.andExpect(jsonPath("$.data.expiresIn").value(300))
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						containsString("oauth_state=state-value")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
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

		mockMvc.perform(post("/api/auth/oauth")
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
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=1209600")));

		verify(authService).oauthLoginWithTokens(any(), eq("state-value"));
	}

	@Test
	void logoutRevokesRefreshTokenAndDeletesCookie() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token")))
				.andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						containsString("refresh_token=;")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

		verify(authService).logout("refresh-token");
	}
}

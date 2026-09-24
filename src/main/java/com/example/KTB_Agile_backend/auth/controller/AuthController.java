package com.example.KTB_Agile_backend.auth.controller;

import com.example.KTB_Agile_backend.auth.dto.request.OAuthLoginRequest;
import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;
import com.example.KTB_Agile_backend.auth.dto.response.OAuthStateResponse;
import com.example.KTB_Agile_backend.auth.dto.response.TokenReissueResponse;
import com.example.KTB_Agile_backend.auth.service.AuthService;
import com.example.KTB_Agile_backend.auth.service.AuthTokenResult;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final String OAUTH_STATE_COOKIE = "oauth_state";
	private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
	private static final String COOKIE_PATH = "/auth";

	private final AuthService authService;
	private final Duration oauthStateTtl;
	private final Duration refreshTokenTtl;
	private final boolean secureCookies;
	private final URI frontendRedirectUri;

	public AuthController(
			AuthService authService,
			@Value("${auth.refresh-token-ttl-days}") long refreshTokenTtlDays,
			@Value("${auth.oauth.state-ttl-seconds}") long stateTtlSeconds,
			@Value("${auth.cookie.secure:false}") boolean secureCookies,
			@Value("${auth.oauth.frontend-redirect-uri}") String frontendRedirectUri
	) {
		this.authService = authService;
		this.oauthStateTtl = Duration.ofSeconds(stateTtlSeconds);
		this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
		this.secureCookies = secureCookies;
		this.frontendRedirectUri = URI.create(frontendRedirectUri);
	}

	@GetMapping("/oauth/state")
	public ResponseEntity<ApiResponse<OAuthStateResponse>> issueOAuthState() {
		String state = authService.issueOAuthState();
		OAuthStateResponse response = new OAuthStateResponse(state, oauthStateTtl.toSeconds());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, stateCookie(state).toString())
				.body(new ApiResponse<>(response, null));
	}

	@PostMapping("/oauth")
	public ResponseEntity<ApiResponse<AuthResponse>> oauthLogin(
			@Valid @RequestBody OAuthLoginRequest request,
			@CookieValue(name = OAUTH_STATE_COOKIE, required = false) String stateCookie
	) {
		return login(request, stateCookie);
	}

	@GetMapping("/kakao/callback")
	public ResponseEntity<Void> kakaoCallback(
			@RequestParam("code") String code,
			@RequestParam("state") String state,
			@CookieValue(name = OAUTH_STATE_COOKIE, required = false) String stateCookie
	) {
		AuthTokenResult result = authService.oauthLoginWithTokens(
				new OAuthLoginRequest("KAKAO", code, state),
				stateCookie
		);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(frontendRedirectUri)
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
				.build();
	}

	private ResponseEntity<ApiResponse<AuthResponse>> login(
			OAuthLoginRequest request,
			String stateCookie
	) {
		AuthTokenResult result = authService.oauthLoginWithTokens(request, stateCookie);
		HttpStatus status = result.response().isNewUser() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
				.body(new ApiResponse<>(result.response(), null));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenReissueResponse>> reissueToken(
			@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
	) {
		TokenReissueResponse response = authService.reissueToken(refreshToken);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
	) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie().toString())
				.build();
	}

	private ResponseCookie stateCookie(String state) {
		return ResponseCookie.from(OAUTH_STATE_COOKIE, state)
				.httpOnly(true)
				.secure(secureCookies)
				.sameSite("Lax")
				.path(COOKIE_PATH)
				.maxAge(oauthStateTtl)
				.build();
	}

	private ResponseCookie refreshTokenCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
				.httpOnly(true)
				.secure(secureCookies)
				.sameSite("Lax")
				.path(COOKIE_PATH)
				.maxAge(refreshTokenTtl)
				.build();
	}

	private ResponseCookie deleteRefreshTokenCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
				.httpOnly(true)
				.secure(secureCookies)
				.sameSite("Lax")
				.path(COOKIE_PATH)
				.maxAge(Duration.ZERO)
				.build();
	}
}

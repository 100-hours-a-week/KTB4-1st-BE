package com.example.KTB_Agile_backend.auth.client;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.auth.dto.provider.KakaoTokenResponse;
import com.example.KTB_Agile_backend.auth.dto.provider.KakaoUserResponse;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorDetail;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
@Component
public class KakaoOAuthProviderClient implements OAuthProviderClient {

	private static final String PROVIDER_NAME = "KAKAO";

	private final RestClient restClient = RestClient.create();
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;

	public KakaoOAuthProviderClient(
			@Value("${auth.oauth.kakao.client-id}") String clientId,
			@Value("${auth.oauth.kakao.client-secret}") String clientSecret,
			@Value("${auth.oauth.kakao.redirect-uri}") String redirectUri
	) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
	}

	@Override
	public String provider() {
		return PROVIDER_NAME;
	}

	@Override
	public OAuthUserInfo getUserInfo(String authorizationCode) {
		if (authorizationCode == null || authorizationCode.isBlank()) {
			throw new ApiException(
					ErrorCode.AUTH_OAUTH_CODE_INVALID,
					List.of(new ErrorDetail("authorizationCode", "유효하지 않은 인가 코드입니다."))
			);
		}
		if (clientId.isBlank()) {
			throw new IllegalStateException("Kakao OAuth client id is missing");
		}

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("code", authorizationCode);
		if (!clientSecret.isBlank()) {
			form.add("client_secret", clientSecret);
		}
		if (!redirectUri.isBlank()) {
			form.add("redirect_uri", redirectUri);
		}

		KakaoTokenResponse tokenResponse;
		try {
			tokenResponse = restClient.post()
					.uri("https://kauth.kakao.com/oauth/token")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(KakaoTokenResponse.class);
		} catch (RestClientResponseException exception) {
			throw authenticationFailed(exception);
		}
		String accessToken = tokenResponse == null ? null : tokenResponse.accessToken();
		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalStateException("Kakao access token was not returned");
		}

		KakaoUserResponse userResponse;
		try {
			userResponse = restClient.get()
					.uri("https://kapi.kakao.com/v2/user/me")
					.headers(headers -> headers.setBearerAuth(accessToken))
					.retrieve()
					.body(KakaoUserResponse.class);
		} catch (RestClientResponseException exception) {
			throw authenticationFailed(exception);
		}
		String providerUserId = userResponse == null || userResponse.id() == null
				? null
				: String.valueOf(userResponse.id());
		if (providerUserId == null || providerUserId.isBlank()) {
			throw new IllegalStateException("Kakao user id was not returned");
		}

		KakaoUserResponse.KakaoAccount kakaoAccount = userResponse.kakaoAccount();
		KakaoUserResponse.Profile profile = kakaoAccount == null ? null : kakaoAccount.profile();
		String nickname = firstNonBlank(
				profile == null ? null : profile.nickname(),
				"kakao-" + providerUserId
		);
		String profileImageUrl = firstNonBlank(
				profile == null ? null : profile.profileImageUrl()
		);
		return new OAuthUserInfo(PROVIDER_NAME, providerUserId, nickname, profileImageUrl);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static ApiException authenticationFailed(Throwable cause) {
		return new ApiException(
				ErrorCode.AUTH_OAUTH_AUTHENTICATION_FAILED,
				List.of(new ErrorDetail("authorizationCode", "인가 코드가 만료되었거나 유효하지 않습니다.")),
				cause
		);
	}
}

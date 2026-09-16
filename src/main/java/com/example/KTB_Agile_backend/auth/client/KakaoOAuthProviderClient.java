package com.example.KTB_Agile_backend.auth.client;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KakaoOAuthProviderClient implements OAuthProviderClient {

	private static final String PROVIDER = "KAKAO";

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
		return PROVIDER;
	}

	@Override
	public OAuthUserInfo getUserInfo(String authorizationCode) {
		if (authorizationCode == null || authorizationCode.isBlank() || clientId.isBlank()) {
			throw new IllegalArgumentException("Kakao OAuth configuration or authorization code is missing");
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

		Map<String, Object> tokenResponse = restClient.post()
				.uri("https://kauth.kakao.com/oauth/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		String accessToken = text(tokenResponse, "access_token");
		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalStateException("Kakao access token was not returned");
		}

		Map<String, Object> userResponse = restClient.get()
				.uri("https://kapi.kakao.com/v2/user/me")
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		String providerUserId = text(userResponse, "id");
		if (providerUserId == null || providerUserId.isBlank()) {
			throw new IllegalStateException("Kakao user id was not returned");
		}

		Map<String, Object> properties = map(userResponse, "properties");
		Map<String, Object> kakaoAccount = map(userResponse, "kakao_account");
		Map<String, Object> profile = map(kakaoAccount, "profile");
		String nickname = firstNonBlank(
				text(properties, "nickname"),
				text(profile, "nickname"),
				"kakao-" + providerUserId
		);
		String profileImageUrl = firstNonBlank(
				text(properties, "profile_image"),
				text(profile, "profile_image_url")
		);
		return new OAuthUserInfo(PROVIDER, providerUserId, nickname, profileImageUrl);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(Map<String, Object> source, String key) {
		Object value = source == null ? null : source.get(key);
		return value instanceof Map<?, ?> valueMap
				? (Map<String, Object>) valueMap
				: Map.of();
	}

	private static String text(Map<String, Object> source, String key) {
		Object value = source == null ? null : source.get(key);
		return value == null ? null : String.valueOf(value);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}

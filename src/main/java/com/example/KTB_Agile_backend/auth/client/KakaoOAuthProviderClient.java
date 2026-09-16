package com.example.KTB_Agile_backend.auth.client;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;
import com.example.KTB_Agile_backend.auth.dto.provider.KakaoTokenResponse;
import com.example.KTB_Agile_backend.auth.dto.provider.KakaoUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

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

		KakaoTokenResponse tokenResponse = restClient.post()
				.uri("https://kauth.kakao.com/oauth/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(KakaoTokenResponse.class);
		String accessToken = tokenResponse == null ? null : tokenResponse.accessToken();
		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalStateException("Kakao access token was not returned");
		}

		KakaoUserResponse userResponse = restClient.get()
				.uri("https://kapi.kakao.com/v2/user/me")
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(KakaoUserResponse.class);
		String providerUserId = userResponse == null || userResponse.id() == null
				? null
				: String.valueOf(userResponse.id());
		if (providerUserId == null || providerUserId.isBlank()) {
			throw new IllegalStateException("Kakao user id was not returned");
		}

		KakaoUserResponse.Properties properties = userResponse.properties();
		KakaoUserResponse.KakaoAccount kakaoAccount = userResponse.kakaoAccount();
		KakaoUserResponse.Profile profile = kakaoAccount == null ? null : kakaoAccount.profile();
		String nickname = firstNonBlank(
				properties == null ? null : properties.nickname(),
				profile == null ? null : profile.nickname(),
				"kakao-" + providerUserId
		);
		String profileImageUrl = firstNonBlank(
				properties == null ? null : properties.profileImageUrl(),
				profile == null ? null : profile.profileImageUrl()
		);
		return new OAuthUserInfo(PROVIDER, providerUserId, nickname, profileImageUrl);
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

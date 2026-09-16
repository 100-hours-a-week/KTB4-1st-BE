package com.example.KTB_Agile_backend.auth.client;

import com.example.KTB_Agile_backend.auth.dto.OAuthUserInfo;

public interface OAuthProviderClient {

	String provider();

	OAuthUserInfo getUserInfo(String authorizationCode);
}

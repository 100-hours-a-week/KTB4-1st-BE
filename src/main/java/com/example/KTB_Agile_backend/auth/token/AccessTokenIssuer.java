package com.example.KTB_Agile_backend.auth.token;

import com.example.KTB_Agile_backend.user.entity.User;

public interface AccessTokenIssuer {

	String issueAccessToken(User user);

	long accessTokenExpiresInSeconds();
}

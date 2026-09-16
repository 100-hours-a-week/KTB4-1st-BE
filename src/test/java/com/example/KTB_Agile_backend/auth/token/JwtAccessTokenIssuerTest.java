package com.example.KTB_Agile_backend.auth.token;

import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtAccessTokenIssuerTest {

	private static final String SECRET = "test-only-jwt-secret-change-me-please-32";

	@Test
	void issuesSignedAccessTokenWithRoleAndConfiguredExpiration() {
		JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(SECRET, 900);
		String token = issuer.issue(new User("kim"));

		SecretKeySpec secretKey = new SecretKeySpec(
				SECRET.getBytes(StandardCharsets.UTF_8),
				"HmacSHA256"
		);
		Jwt jwt = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build()
				.decode(token);

		assertEquals("USER", jwt.getClaimAsString("role"));
		assertEquals(900, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds());
		assertEquals(900, issuer.expiresInSeconds());
	}
}

package com.example.KTB_Agile_backend.auth.token;

import com.example.KTB_Agile_backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final Duration accessTokenTtl;

	public JwtAccessTokenIssuer(
			@Value("${auth.jwt.secret}") String jwtSecret,
			@Value("${auth.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds
	) {
		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
		}
		this.jwtEncoder = NimbusJwtEncoder.withSecretKey(
				new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
		).build();
		this.accessTokenTtl = Duration.ofSeconds(accessTokenTtlSeconds);
	}

	@Override
	public String issue(User user) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(String.valueOf(user.getId()))
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(accessTokenTtl))
				.claim("role", user.getUserRole().name())
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims
		)).getTokenValue();
	}

	@Override
	public long expiresInSeconds() {
		return accessTokenTtl.toSeconds();
	}
}

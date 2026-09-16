package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class TokenService {

	public static final String TOKEN_TYPE = "Bearer";

	private final JwtEncoder jwtEncoder;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;
	private final SecureRandom secureRandom = new SecureRandom();

	public TokenService(
			@Value("${auth.jwt.secret}") String jwtSecret,
			@Value("${auth.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
			@Value("${auth.refresh-token-ttl-days}") long refreshTokenTtlDays
	) {
		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
		}
		this.jwtEncoder = NimbusJwtEncoder.withSecretKey(
				new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
		).build();
		this.accessTokenTtl = Duration.ofSeconds(accessTokenTtlSeconds);
		this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
	}

	public String issueAccessToken(User user) {
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

	public String issueRefreshToken() {
		byte[] tokenBytes = new byte[32];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	public LocalDateTime refreshTokenExpiresAt() {
		return LocalDateTime.now().plus(refreshTokenTtl);
	}

	public long accessTokenExpiresInSeconds() {
		return accessTokenTtl.toSeconds();
	}
}

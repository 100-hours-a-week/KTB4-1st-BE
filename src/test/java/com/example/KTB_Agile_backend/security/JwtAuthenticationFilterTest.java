package com.example.KTB_Agile_backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

	private static final String SECRET = "test-only-jwt-secret-change-me-please-32";

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void storesAuthenticatedUserFromValidBearerToken() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(decoder());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token());

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertEquals("42", authentication.getPrincipal());
		assertTrue(authentication.getAuthorities().stream().anyMatch(
				authority -> authority.getAuthority().equals("ROLE_USER")
		));
	}

	@Test
	void ignoresInvalidBearerToken() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(decoder());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
	}

	private static JwtDecoder decoder() {
		return NimbusJwtDecoder.withSecretKey(key()).macAlgorithm(MacAlgorithm.HS256).build();
	}

	private static String token() {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("42")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900))
				.claim("role", "USER")
				.build();
		JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(key()).build();
		return encoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims
		)).getTokenValue();
	}

	private static SecretKeySpec key() {
		return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}

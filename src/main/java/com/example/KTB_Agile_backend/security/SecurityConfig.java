package com.example.KTB_Agile_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	JwtDecoder jwtDecoder(@Value("${auth.jwt.secret}") String jwtSecret) {
		byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
		if (secret.length < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
		}

		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
				new SecretKeySpec(secret, "HmacSHA256")
		).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefault());
		return decoder;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/oauth/**", "/api/auth/refresh", "/auth/**", "/error").permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(
						new JwtAuthenticationFilter(jwtDecoder),
						UsernamePasswordAuthenticationFilter.class
				);
		return http.build();
	}
}

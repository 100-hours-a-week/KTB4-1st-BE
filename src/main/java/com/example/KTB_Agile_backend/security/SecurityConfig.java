package com.example.KTB_Agile_backend.security;

import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final int MIN_JWT_SECRET_BYTES = 32;

	@Bean
	JwtDecoder jwtDecoder(@Value("${auth.jwt.secret}") String jwtSecret) {
		byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
		if (secret.length < MIN_JWT_SECRET_BYTES) {
			throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
		}

		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
				new SecretKeySpec(secret, "HmacSHA256")
		).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefault());
		return decoder;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${cors.allowed-origins:http://127.0.0.1:3000}") List<String> allowedOrigins
	) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtDecoder jwtDecoder,
			ObjectMapper objectMapper
	) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/oauth/**", "/auth/kakao/callback", "/auth/refresh", "/error").permitAll()
						.anyRequest().authenticated()
				)
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, cause) -> writeError(
								response,
								objectMapper,
								ErrorCode.AUTHENTICATION_REQUIRED
						))
						.accessDeniedHandler((request, response, cause) -> writeError(
								response,
								objectMapper,
								ErrorCode.FORBIDDEN
						))
				)
				.addFilterBefore(
						new JwtAuthenticationFilter(jwtDecoder),
						UsernamePasswordAuthenticationFilter.class
				);
		return http.build();
	}

	private static void writeError(
			HttpServletResponse response,
			ObjectMapper objectMapper,
			ErrorCode code
	) throws IOException {
		response.setStatus(code.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(
				response.getWriter(),
				new ApiResponse<Void>(null, new ErrorResponse(code.value(), code.message(), List.of()))
		);
	}
}

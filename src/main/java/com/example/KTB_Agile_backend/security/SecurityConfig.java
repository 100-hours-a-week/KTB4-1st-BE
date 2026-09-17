package com.example.KTB_Agile_backend.security;

import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
import java.io.IOException;
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
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtDecoder jwtDecoder,
			ObjectMapper objectMapper
	) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/oauth/**", "/auth/refresh", "/error").permitAll()
						.anyRequest().authenticated()
				)
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, cause) -> writeError(
								response,
								objectMapper,
								ErrorCode.UNAUTHORIZED,
								unauthorizedMessage(request)
						))
						.accessDeniedHandler((request, response, cause) -> writeError(
								response,
								objectMapper,
								ErrorCode.FORBIDDEN,
								"접근 권한이 없습니다."
						))
				)
				.addFilterBefore(
						new JwtAuthenticationFilter(jwtDecoder),
						UsernamePasswordAuthenticationFilter.class
				);
		return http.build();
	}

	private static String unauthorizedMessage(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		return requestUri != null && requestUri.endsWith("/auth/logout")
				? "로그인이 필요하거나 Access Token이 만료되었거나 유효하지 않습니다."
				: "로그인이 필요합니다.";
	}

	private static void writeError(
			HttpServletResponse response,
			ObjectMapper objectMapper,
			ErrorCode code,
			String message
	) throws IOException {
		response.setStatus(code.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(
				response.getWriter(),
				new ApiResponse<Void>(null, new ErrorResponse(code.value(), message, java.util.List.of()))
		);
	}
}

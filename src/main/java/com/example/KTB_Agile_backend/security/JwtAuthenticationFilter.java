package com.example.KTB_Agile_backend.security;

import com.example.KTB_Agile_backend.user.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtDecoder jwtDecoder;

	public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				Jwt jwt = jwtDecoder.decode(token);
				String subject = jwt.getSubject();
				String role = jwt.getClaimAsString("role");
				if (subject == null || subject.isBlank() || Long.parseLong(subject) < 1
						|| role == null || role.isBlank()) {
					throw new IllegalArgumentException("invalid JWT claims");
				}
				UserRole userRole = UserRole.valueOf(role);
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(
								subject,
								null,
								List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name()))
						)
				);
			} catch (JwtException | IllegalArgumentException exception) {
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}

	private static String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return null;
		}

		String token = header.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}
}

package com.example.KTB_Agile_backend.chat.security;

import com.example.KTB_Agile_backend.user.entity.UserRole;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtStompAuthenticationInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtDecoder jwtDecoder;

	public JwtStompAuthenticationInterceptor(JwtDecoder jwtDecoder) {
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			accessor.setUser(authenticate(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)));
		}
		return message;
	}

	private Authentication authenticate(String authorization) {
		if (authorization == null || !authorization.regionMatches(
				true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			throw new BadCredentialsException("Bearer token is required");
		}

		try {
			Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()).trim());
			String subject = jwt.getSubject();
			String role = jwt.getClaimAsString("role");
			if (subject == null || subject.isBlank() || Long.parseLong(subject) < 1
					|| role == null || role.isBlank()) {
				throw new BadCredentialsException("Invalid JWT claims");
			}
			UserRole userRole = UserRole.valueOf(role);
			return UsernamePasswordAuthenticationToken.authenticated(
				subject, null, List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name())));
		} catch (JwtException | IllegalArgumentException exception) {
			throw new BadCredentialsException("Invalid bearer token", exception);
		}
	}
}

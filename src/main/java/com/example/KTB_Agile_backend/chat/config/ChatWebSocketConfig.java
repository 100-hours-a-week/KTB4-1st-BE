package com.example.KTB_Agile_backend.chat.config;

import com.example.KTB_Agile_backend.chat.security.ChatRoomAccessInterceptor;
import com.example.KTB_Agile_backend.chat.security.JwtStompAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtStompAuthenticationInterceptor authenticationInterceptor;
	private final ChatRoomAccessInterceptor chatRoomAccessInterceptor;
	private final List<String> allowedOrigins;

	public ChatWebSocketConfig(
			JwtStompAuthenticationInterceptor authenticationInterceptor,
			ChatRoomAccessInterceptor chatRoomAccessInterceptor,
			@Value("${cors.allowed-origins:http://127.0.0.1:3000}") List<String> allowedOrigins
	) {
		this.authenticationInterceptor = authenticationInterceptor;
		this.chatRoomAccessInterceptor = chatRoomAccessInterceptor;
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authenticationInterceptor, chatRoomAccessInterceptor);
	}
}

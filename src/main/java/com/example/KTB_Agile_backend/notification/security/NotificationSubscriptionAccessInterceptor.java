package com.example.KTB_Agile_backend.notification.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class NotificationSubscriptionAccessInterceptor implements ChannelInterceptor {

	private static final String USER_NOTIFICATION_DESTINATION = "/user/queue/notifications";

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}

		String destination = accessor.getDestination();
		if (StompCommand.SEND.equals(accessor.getCommand()) && destination != null
				&& (destination.startsWith("/user/") || destination.startsWith("/queue/"))) {
			throw new AccessDeniedException("Clients cannot send directly to user or queue destinations.");
		}
		if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			return message;
		}

		if (destination != null && (destination.startsWith("/user/") || destination.startsWith("/queue/"))) {
			if (!USER_NOTIFICATION_DESTINATION.equals(destination) || accessor.getUser() == null) {
				throw new AccessDeniedException("Only authenticated users may subscribe to their notification queue.");
			}
		}
		return message;
	}
}

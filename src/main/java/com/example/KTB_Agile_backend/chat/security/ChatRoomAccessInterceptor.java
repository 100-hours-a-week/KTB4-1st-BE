package com.example.KTB_Agile_backend.chat.security;

import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class ChatRoomAccessInterceptor implements ChannelInterceptor {

	private static final String ROOM_TOPIC_PREFIX = "/topic/chat/rooms/";

	private final ChatMemberRepository chatMemberRepository;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}

		String destination = accessor.getDestination();
		if (StompCommand.SEND.equals(accessor.getCommand())
				&& destination != null && destination.startsWith("/topic/")) {
			throw new AccessDeniedException("클라이언트는 채팅방 topic에 직접 메시지를 보낼 수 없습니다.");
		}
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
				&& destination != null && destination.startsWith(ROOM_TOPIC_PREFIX)) {
			ensureRoomMember(accessor, destination);
		}
		return message;
	}

	private void ensureRoomMember(StompHeaderAccessor accessor, String destination) {
		Principal principal = accessor.getUser();
		if (principal == null) {
			throw new AccessDeniedException("인증이 필요합니다.");
		}

		try {
			long chatRoomId = Long.parseLong(destination.substring(ROOM_TOPIC_PREFIX.length()));
			long userId = Long.parseLong(principal.getName());
			if (chatRoomId < 1 || userId < 1
					|| chatMemberRepository.findActiveMember(chatRoomId, userId).isEmpty()) {
				throw new AccessDeniedException("채팅방 멤버만 구독할 수 있습니다.");
			}
		} catch (NumberFormatException exception) {
			throw new AccessDeniedException("채팅방 주소가 올바르지 않습니다.", exception);
		}
	}
}

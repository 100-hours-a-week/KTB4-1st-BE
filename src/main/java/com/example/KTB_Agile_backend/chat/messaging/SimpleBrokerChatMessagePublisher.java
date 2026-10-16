package com.example.KTB_Agile_backend.chat.messaging;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimpleBrokerChatMessagePublisher implements ChatMessagePublisher {

	private final SimpMessagingTemplate messagingTemplate;

	@Override
	public void publish(ChatMessageResponse message) {
		messagingTemplate.convertAndSend("/topic/chat/rooms/" + message.chatRoomId(), message);
	}
}

package com.example.KTB_Agile_backend.chat.controller;

import com.example.KTB_Agile_backend.chat.dto.request.ChatMessageSendRequest;
import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.messaging.ChatMessagePublisher;
import com.example.KTB_Agile_backend.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageStompController {

	private final ChatMessageService chatMessageService;
	private final ChatMessagePublisher chatMessagePublisher;

	@MessageMapping("/chat/rooms/{chatRoomId}/messages")
	public void send(
			@DestinationVariable Long chatRoomId,
			@Payload ChatMessageSendRequest request,
			Principal principal
	) {
		ChatMessageResponse response = chatMessageService.send(
				chatRoomId, Long.valueOf(principal.getName()), request == null ? null : request.content());
		chatMessagePublisher.publish(response);
	}
}

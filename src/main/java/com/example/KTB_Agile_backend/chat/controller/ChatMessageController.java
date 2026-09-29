package com.example.KTB_Agile_backend.chat.controller;

import com.example.KTB_Agile_backend.chat.dto.request.ChatMessageSendRequest;
import com.example.KTB_Agile_backend.chat.dto.response.ChatMessagePageResponse;
import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.service.ChatMessageService;
import com.example.KTB_Agile_backend.chat.service.ChatMessageQueryService;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;
	private final ChatMessageQueryService chatMessageQueryService;
	private final SimpMessagingTemplate messagingTemplate;

	@GetMapping("/chat/rooms/{chatRoomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessagePageResponse>> findMessages(
			@PathVariable Long chatRoomId,
			@RequestParam(required = false) String cursor,
			Authentication authentication
	) {
		Long userId = Long.valueOf(authentication.getName());
		ChatMessagePageResponse response = chatMessageQueryService.findMessages(chatRoomId, userId, cursor);
		if ((cursor == null || cursor.isBlank()) && !response.messages().isEmpty()) {
			Long latestMessageId = response.messages().get(response.messages().size() - 1).messageId();
			chatMessageService.markReadThrough(chatRoomId, userId, latestMessageId);
		}
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@MessageMapping("/chat/rooms/{chatRoomId}/messages")
	public void send(
			@DestinationVariable Long chatRoomId,
			@Payload ChatMessageSendRequest request,
			Principal principal
	) {
		ChatMessageResponse response = chatMessageService.send(
				chatRoomId, Long.valueOf(principal.getName()), request == null ? null : request.content());
		messagingTemplate.convertAndSend("/topic/chat/rooms/" + chatRoomId, response);
	}
}

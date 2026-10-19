package com.example.KTB_Agile_backend.chat.controller;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessagePageResponse;
import com.example.KTB_Agile_backend.chat.service.ChatMessageService;
import com.example.KTB_Agile_backend.chat.service.ChatMessageQueryService;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;
	private final ChatMessageQueryService chatMessageQueryService;

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
}

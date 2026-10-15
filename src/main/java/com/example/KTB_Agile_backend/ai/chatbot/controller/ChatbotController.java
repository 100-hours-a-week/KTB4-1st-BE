package com.example.KTB_Agile_backend.ai.chatbot.controller;

import com.example.KTB_Agile_backend.ai.chatbot.dto.request.ChatbotAskRequest;
import com.example.KTB_Agile_backend.ai.chatbot.dto.response.ChatbotAskResponse;
import com.example.KTB_Agile_backend.ai.chatbot.service.ChatbotService;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
	private final ChatbotService chatbotService;

	@PostMapping("/ask")
	public ResponseEntity<ApiResponse<ChatbotAskResponse>> ask(
			Authentication authentication,
			@Valid @RequestBody ChatbotAskRequest request
	) {
		Long userId = Long.valueOf(authentication.getName());
		return ResponseEntity.ok(new ApiResponse<>(chatbotService.ask(userId, request), null));
	}
}

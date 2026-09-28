package com.example.KTB_Agile_backend.chat.controller;

import com.example.KTB_Agile_backend.chat.dto.response.ChatRoomPageResponse;
import com.example.KTB_Agile_backend.chat.service.ChatRoomQueryService;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

	private final ChatRoomQueryService chatRoomQueryService;

	@GetMapping("/chat-rooms")
	public ResponseEntity<ApiResponse<ChatRoomPageResponse>> findChatRooms(
			Authentication authentication,
			@RequestParam(required = false) String direction,
			@RequestParam(defaultValue = "20") String size,
			@RequestParam(required = false) String cursor
	) {
		ChatRoomPageResponse response = chatRoomQueryService.findChatRooms(
				Long.valueOf(authentication.getName()), direction, size, cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

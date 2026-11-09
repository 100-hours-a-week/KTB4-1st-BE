package com.example.KTB_Agile_backend.ai.messagerefine.controller;

import com.example.KTB_Agile_backend.ai.messagerefine.dto.request.MessageRefineRequest;
import com.example.KTB_Agile_backend.ai.messagerefine.dto.response.MessageRefineResponse;
import com.example.KTB_Agile_backend.ai.messagerefine.service.MessageRefineService;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message-refine")
@RequiredArgsConstructor
public class MessageRefineController {

	private final MessageRefineService messageRefineService;

	@PostMapping("/refine")
	public ResponseEntity<ApiResponse<MessageRefineResponse>> refine(
			Authentication authentication,
			@RequestBody MessageRefineRequest request
	) {
		MessageRefineResponse response = messageRefineService.refine(
				Long.valueOf(authentication.getName()), request);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

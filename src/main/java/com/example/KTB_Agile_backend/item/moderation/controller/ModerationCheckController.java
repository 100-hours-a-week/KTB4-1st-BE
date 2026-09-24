package com.example.KTB_Agile_backend.item.moderation.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.item.moderation.dto.request.ModerationCheckRequest;
import com.example.KTB_Agile_backend.item.moderation.dto.response.ModerationCheckResponse;
import com.example.KTB_Agile_backend.item.moderation.service.ModerationCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ModerationCheckController {

	private final ModerationCheckService moderationCheckService;

	@PostMapping("/moderation-checks")
	public ResponseEntity<ApiResponse<ModerationCheckResponse>> check(
			Authentication authentication,
			@Valid @RequestBody ModerationCheckRequest request
	) {
		ModerationCheckResponse response = moderationCheckService.check(
				Long.valueOf(authentication.getName()), request);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

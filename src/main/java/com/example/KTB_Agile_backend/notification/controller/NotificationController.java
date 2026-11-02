package com.example.KTB_Agile_backend.notification.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.notification.dto.NotificationPageResponse;
import com.example.KTB_Agile_backend.notification.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationQueryService notificationQueryService;

	@GetMapping
	public ResponseEntity<ApiResponse<NotificationPageResponse>> findMine(
			Authentication authentication,
			@RequestParam(defaultValue = "10") String size,
			@RequestParam(required = false) String cursor
	) {
		NotificationPageResponse response = notificationQueryService.findMine(
				Long.valueOf(authentication.getName()), size, cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

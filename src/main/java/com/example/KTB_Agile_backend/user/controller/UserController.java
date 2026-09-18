package com.example.KTB_Agile_backend.user.controller;

import com.example.KTB_Agile_backend.user.dto.request.UserPreferenceRequest;
import com.example.KTB_Agile_backend.user.dto.response.UserPreferenceResponse;
import com.example.KTB_Agile_backend.user.service.AccountWithdrawalService;
import com.example.KTB_Agile_backend.user.service.UserPreferenceService;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final AccountWithdrawalService accountWithdrawalService;
	private final UserPreferenceService userPreferenceService;

	@DeleteMapping
	public ResponseEntity<Void> withdraw(Authentication authentication) {
		accountWithdrawalService.withdraw(Long.valueOf(authentication.getName()));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/preferences")
	public ResponseEntity<ApiResponse<UserPreferenceResponse>> createPreferences(
			Authentication authentication,
			@Valid @RequestBody UserPreferenceRequest request
	) {
		UserPreferenceResponse response = userPreferenceService.create(
				Long.valueOf(authentication.getName()),
				request
		);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new ApiResponse<>(response, null));
	}

	@PutMapping("/preferences")
	public ResponseEntity<Void> updatePreferences(
			Authentication authentication,
			@Valid @RequestBody UserPreferenceRequest request
	) {
		userPreferenceService.update(Long.valueOf(authentication.getName()), request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/preferences")
	public ResponseEntity<UserPreferenceResponse> getPreferences(Authentication authentication) {
		return ResponseEntity.ok(userPreferenceService.get(Long.valueOf(authentication.getName())));
	}
}

package com.example.KTB_Agile_backend.search.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.search.dto.request.SmartSearchRequest;
import com.example.KTB_Agile_backend.search.dto.response.SmartSearchResponse;
import com.example.KTB_Agile_backend.search.service.SmartSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SmartSearchController {

	private final SmartSearchService smartSearchService;

	@PostMapping("/rank-candidates")
	public ResponseEntity<ApiResponse<SmartSearchResponse>> search(
			Authentication authentication,
			@Valid @RequestBody SmartSearchRequest request
	) {
		SmartSearchResponse response = smartSearchService.search(
				Long.valueOf(authentication.getName()), request);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

package com.example.KTB_Agile_backend.search.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.search.dto.response.SearchHistoryPageResponse;
import com.example.KTB_Agile_backend.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search-histories")
@RequiredArgsConstructor
public class SearchHistoryController {

	private final SearchHistoryService searchHistoryService;

	@GetMapping
	public ResponseEntity<ApiResponse<SearchHistoryPageResponse>> find(
			Authentication authentication,
			@RequestParam(required = false) String cursor
	) {
		SearchHistoryPageResponse response = searchHistoryService.findByUser(
				Long.valueOf(authentication.getName()), cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@DeleteMapping("/{searchHistoryId}")
	public ResponseEntity<Void> deleteOne(
			Authentication authentication,
			@PathVariable Long searchHistoryId
	) {
		searchHistoryService.deleteOne(Long.valueOf(authentication.getName()), searchHistoryId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	public ResponseEntity<Void> deleteAll(Authentication authentication) {
		searchHistoryService.deleteAll(Long.valueOf(authentication.getName()));
		return ResponseEntity.noContent().build();
	}
}

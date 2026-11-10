package com.example.KTB_Agile_backend.search.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.search.dto.response.SearchRankingResponse;
import com.example.KTB_Agile_backend.search.service.SearchRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchRankingController {

	private final SearchRankingService searchRankingService;

	@GetMapping("/rankings")
	public ResponseEntity<ApiResponse<SearchRankingResponse>> findToday() {
		return ResponseEntity.ok(new ApiResponse<>(searchRankingService.findToday(), null));
	}
}

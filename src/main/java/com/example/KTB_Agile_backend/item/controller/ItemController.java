package com.example.KTB_Agile_backend.item.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.item.dto.request.CreateItemRequest;
import com.example.KTB_Agile_backend.item.dto.response.ItemCreateResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemDetailResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemPageResponse;
import com.example.KTB_Agile_backend.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ItemController {

	private final ItemService itemService;

	@PostMapping("/items")
	public ResponseEntity<ApiResponse<ItemCreateResponse>> create(
			Authentication authentication,
			@Valid @RequestBody CreateItemRequest request
	) {
		ItemCreateResponse response = itemService.create(Long.valueOf(authentication.getName()), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response, null));
	}

	@GetMapping("/items/{itemId}")
	public ResponseEntity<ApiResponse<ItemDetailResponse>> findDetail(
			Authentication authentication,
			@PathVariable Long itemId
	) {
		ItemDetailResponse response = itemService.findDetail(
				Long.valueOf(authentication.getName()), itemId);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@GetMapping("/groups/{groupId}/items")
	public ResponseEntity<ApiResponse<ItemPageResponse>> findByGroup(
			Authentication authentication,
			@PathVariable Long groupId,
			@RequestParam(required = false) String cursor
	) {
		ItemPageResponse response = itemService.findByGroup(
				Long.valueOf(authentication.getName()), groupId, cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

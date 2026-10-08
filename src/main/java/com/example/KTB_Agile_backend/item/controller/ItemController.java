package com.example.KTB_Agile_backend.item.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.item.dto.request.CreateItemRequest;
import com.example.KTB_Agile_backend.item.dto.request.UpdateItemRequest;
import com.example.KTB_Agile_backend.item.dto.response.ItemCreateResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemDetailResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemLikeResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemPageResponse;
import com.example.KTB_Agile_backend.item.dto.response.MyItemPageResponse;
import com.example.KTB_Agile_backend.item.event.ItemViewEvent;
import com.example.KTB_Agile_backend.item.event.ItemViewEventPublisher;
import com.example.KTB_Agile_backend.item.service.ItemLikeService;
import com.example.KTB_Agile_backend.item.service.ItemService;
import com.example.KTB_Agile_backend.item.service.ItemQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ItemController {

	private final ItemService itemService;
	private final ItemLikeService itemLikeService;
	private final ItemQueryService itemQueryService;
	private final ItemViewEventPublisher itemViewEventPublisher;

	@GetMapping("/items")
	public ResponseEntity<ApiResponse<ItemPageResponse>> search(
			Authentication authentication,
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(required = false) String cursor
	) {
		ItemPageResponse response = itemQueryService.search(
				Long.valueOf(authentication.getName()), keyword, cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@PostMapping("/items")
	public ResponseEntity<ApiResponse<ItemCreateResponse>> create(
			Authentication authentication,
			@Valid @RequestBody CreateItemRequest request
	) {
		ItemCreateResponse response = itemService.create(Long.valueOf(authentication.getName()), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response, null));
	}

	@PostMapping("/items/{itemId}/likes")
	public ResponseEntity<ApiResponse<ItemLikeResponse>> like(
			Authentication authentication,
			@PathVariable Long itemId
	) {
		ItemLikeResponse response = itemLikeService.like(Long.valueOf(authentication.getName()), itemId);
		return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response, null));
	}

	@DeleteMapping("/items/{itemId}/likes")
	public ResponseEntity<Void> unlike(
			Authentication authentication,
			@PathVariable Long itemId
	) {
		itemLikeService.unlike(Long.valueOf(authentication.getName()), itemId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/items/{itemId}")
	public ResponseEntity<Void> update(
			Authentication authentication,
			@PathVariable Long itemId,
			@Valid @RequestBody UpdateItemRequest request
	) {
		itemService.update(Long.valueOf(authentication.getName()), itemId, request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<Void> delete(
			Authentication authentication,
			@PathVariable Long itemId
	) {
		itemService.delete(Long.valueOf(authentication.getName()), itemId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/items/{itemId}")
	public ResponseEntity<ApiResponse<ItemDetailResponse>> findDetail(
			Authentication authentication,
			@PathVariable Long itemId
	) {
		Long userId = Long.valueOf(authentication.getName());
		ItemDetailResponse response = itemQueryService.findDetail(userId, itemId);
		itemViewEventPublisher.publish(new ItemViewEvent(userId, itemId));
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@GetMapping("/users/me/items")
	public ResponseEntity<ApiResponse<MyItemPageResponse>> findMyItems(
			Authentication authentication,
			@RequestParam(defaultValue = "10") String size,
			@RequestParam(required = false) String cursor
	) {
		MyItemPageResponse response = itemQueryService.findMyItems(
				Long.valueOf(authentication.getName()), size, cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@GetMapping("/users/me/liked-items")
	public ResponseEntity<ApiResponse<MyItemPageResponse>> findMyLikedItems(
			Authentication authentication,
			@RequestParam(defaultValue = "10") String size,
			@RequestParam(required = false) String cursor
	) {
		MyItemPageResponse response = itemQueryService.findMyLikedItems(
				Long.valueOf(authentication.getName()), size, cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@GetMapping("/groups/{groupId}/items")
	public ResponseEntity<ApiResponse<ItemPageResponse>> findByGroup(
			Authentication authentication,
			@PathVariable Long groupId,
			@RequestParam(required = false) String cursor
	) {
		ItemPageResponse response = itemQueryService.findByGroup(
				Long.valueOf(authentication.getName()), groupId, cursor);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

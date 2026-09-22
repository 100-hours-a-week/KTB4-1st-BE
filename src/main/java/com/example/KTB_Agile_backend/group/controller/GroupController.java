package com.example.KTB_Agile_backend.group.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.group.dto.request.CreateGroupRequest;
import com.example.KTB_Agile_backend.group.dto.response.GroupPageResponse;
import com.example.KTB_Agile_backend.group.service.GroupQueryService;
import com.example.KTB_Agile_backend.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

	private final GroupService groupService;
	private final GroupQueryService groupQueryService;

	@GetMapping
	public ResponseEntity<ApiResponse<GroupPageResponse>> search(
			Authentication authentication,
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(required = false) String cursor
	) {
		Long userId = Long.valueOf(authentication.getName());
		return ResponseEntity.ok(new ApiResponse<>(groupQueryService.search(userId, keyword, cursor), null));
	}

	@GetMapping("/recommendations")
	public ResponseEntity<ApiResponse<GroupPageResponse>> recommendations(
			Authentication authentication,
			@RequestParam(required = false) String cursor
	) {
		Long userId = Long.valueOf(authentication.getName());
		return ResponseEntity.ok(new ApiResponse<>(groupQueryService.recommendations(userId, cursor), null));
	}

	@PostMapping
	public ResponseEntity<Void> create(
			Authentication authentication,
			@Valid @RequestBody CreateGroupRequest request
	) {
		groupService.create(Long.valueOf(authentication.getName()), request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/{groupId}/members")
	public ResponseEntity<Void> join(
			Authentication authentication,
			@PathVariable Long groupId
	) {
		groupService.join(Long.valueOf(authentication.getName()), groupId);
		return ResponseEntity.created(URI.create("/groups/" + groupId + "/members/me")).build();
	}

	@DeleteMapping("/{groupId}/members/me")
	public ResponseEntity<Void> leave(
			Authentication authentication,
			@PathVariable Long groupId
	) {
		groupService.leave(Long.valueOf(authentication.getName()), groupId);
		return ResponseEntity.noContent().build();
	}
}

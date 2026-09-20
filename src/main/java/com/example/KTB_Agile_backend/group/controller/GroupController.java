package com.example.KTB_Agile_backend.group.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.group.dto.request.CreateGroupRequest;
import com.example.KTB_Agile_backend.group.dto.response.GroupCreatedResponse;
import com.example.KTB_Agile_backend.group.dto.response.GroupPageResponse;
import com.example.KTB_Agile_backend.group.service.GroupQueryService;
import com.example.KTB_Agile_backend.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(required = false) String cursor
	) {
		return ResponseEntity.ok(new ApiResponse<>(groupQueryService.search(keyword, cursor), null));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<GroupCreatedResponse>> create(
			Authentication authentication,
			@Valid @RequestBody CreateGroupRequest request
	) {
		GroupCreatedResponse response = groupService.create(
				Long.valueOf(authentication.getName()), request);

		return ResponseEntity.created(URI.create("/groups/" + response.groupId()))
				.body(new ApiResponse<>(response, null));
	}

	@PostMapping("/{groupId}/members")
	public ResponseEntity<Void> join(
			Authentication authentication,
			@PathVariable Long groupId
	) {
		groupService.join(Long.valueOf(authentication.getName()), groupId);
		return ResponseEntity.noContent().build();
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

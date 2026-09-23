package com.example.KTB_Agile_backend.image.controller;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.image.dto.request.PresignedUploadBatchRequest;
import com.example.KTB_Agile_backend.image.dto.request.PresignedUploadRequest;
import com.example.KTB_Agile_backend.image.dto.response.PresignedUploadResponse;
import com.example.KTB_Agile_backend.image.service.ImageUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

	private final ImageUploadService imageUploadService;

	@PostMapping("/presigned-url")
	public ResponseEntity<ApiResponse<PresignedUploadResponse>> issuePresignedUrl(
			Authentication authentication,
			@Valid @RequestBody PresignedUploadRequest request
	) {
		PresignedUploadResponse response = imageUploadService.issue(
				Long.valueOf(authentication.getName()), request);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	@PostMapping("/presigned-urls")
	public ResponseEntity<ApiResponse<List<PresignedUploadResponse>>> issuePresignedUrls(
			Authentication authentication,
			@Valid @RequestBody PresignedUploadBatchRequest request
	) {
		List<PresignedUploadResponse> response = imageUploadService.issue(
				Long.valueOf(authentication.getName()), request.images());
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}
}

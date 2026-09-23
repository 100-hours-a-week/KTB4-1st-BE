package com.example.KTB_Agile_backend.image.controller;

import com.example.KTB_Agile_backend.image.dto.request.ImageAnalysisRequest;
import com.example.KTB_Agile_backend.image.service.ImageAiAnalysisService;
import com.example.KTB_Agile_backend.image.service.S3ImageObjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageWorkflowController {

	private final ImageAiAnalysisService imageAiAnalysisService;
	private final S3ImageObjectService s3ImageObjectService;

	@PostMapping("/ai-analysis")
	public ResponseEntity<String> analyze(
			Authentication authentication,
			@Valid @RequestBody ImageAnalysisRequest request
	) {
		String result = imageAiAnalysisService.analyze(Long.valueOf(authentication.getName()), request.objectKey());
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
	}

	@DeleteMapping
	public ResponseEntity<Void> cancel(
			Authentication authentication,
			@RequestParam String objectKey
	) {
		s3ImageObjectService.deletePendingObject(Long.valueOf(authentication.getName()), objectKey);
		return ResponseEntity.noContent().build();
	}
}

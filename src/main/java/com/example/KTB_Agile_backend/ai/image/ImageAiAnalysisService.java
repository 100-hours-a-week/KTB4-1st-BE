package com.example.KTB_Agile_backend.ai.image;

import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.image.service.S3ImageObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageAiAnalysisService {

	private final S3ImageObjectService s3ImageObjectService;
	private final AiProperties aiProperties;
	private final RestClient restClient;

	public ResponseEntity<String> analyze(Long userId, List<String> objectKeys) {
		s3ImageObjectService.validatePendingObjects(userId, objectKeys);
		if (aiProperties.imageAnalysisUrl().isBlank()) {
			throw new ApiException(AiErrorCode.AI_ANALYSIS_ENDPOINT_NOT_CONFIGURED);
		}
		List<String> imageUrls = objectKeys.stream()
				.map(s3ImageObjectService::presignedAnalysisUrl)
				.toList();
		try {
			return restClient.post()
					.uri(aiProperties.imageAnalysisUrl())
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("imageUrls", imageUrls))
					.exchange((request, response) -> ResponseEntity.status(response.getStatusCode())
							.contentType(MediaType.APPLICATION_JSON)
							.body(response.bodyTo(String.class)));
		} catch (RestClientException exception) {
			throw new ApiException(AiErrorCode.AI_ANALYSIS_FAILED, List.of(), exception);
		}
	}
}

package com.example.KTB_Agile_backend.ai.image;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.image.service.S3ImageObjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ImageAiAnalysisService {

	private final S3ImageObjectService s3ImageObjectService;
	private final RestClient restClient;
	private final String aiEndpoint;

	public ImageAiAnalysisService(
			S3ImageObjectService s3ImageObjectService,
			@Value("${ai.image-analysis-url:}") String aiEndpoint,
			@Value("${ai.connect-timeout-seconds:5}") long connectTimeoutSeconds,
			@Value("${ai.read-timeout-seconds:90}") long readTimeoutSeconds
	) {
		this.s3ImageObjectService = s3ImageObjectService;
		this.aiEndpoint = aiEndpoint;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
		requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	public ResponseEntity<String> analyze(Long userId, List<String> objectKeys) {
		s3ImageObjectService.validatePendingObjects(userId, objectKeys);
		if (aiEndpoint.isBlank()) {
			throw new ApiException(AiErrorCode.AI_ANALYSIS_ENDPOINT_NOT_CONFIGURED);
		}
		List<String> imageUrls = objectKeys.stream()
				.map(s3ImageObjectService::presignedAnalysisUrl)
				.toList();
		try {
			return restClient.post()
					.uri(aiEndpoint)
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

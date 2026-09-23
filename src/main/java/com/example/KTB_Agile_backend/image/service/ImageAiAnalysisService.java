package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
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

	public String analyze(Long userId, String objectKey) {
		s3ImageObjectService.validatePendingObject(userId, objectKey);
		if (aiEndpoint.isBlank()) {
			throw new ApiException(ErrorCode.AI_ANALYSIS_FAILED, "AI 분석 서버 주소가 설정되지 않았습니다.");
		}
		String imageUrl = s3ImageObjectService.presignedAnalysisUrl(objectKey);
		try {
			return restClient.post()
					.uri(aiEndpoint)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("imageUrl", imageUrl))
					.retrieve()
					.body(String.class);
		} catch (RestClientException exception) {
			throw new ApiException(ErrorCode.AI_ANALYSIS_FAILED, ErrorCode.AI_ANALYSIS_FAILED.message(),
					java.util.List.of(), exception);
		}
	}
}

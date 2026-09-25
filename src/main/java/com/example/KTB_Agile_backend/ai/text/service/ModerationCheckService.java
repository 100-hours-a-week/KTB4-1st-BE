package com.example.KTB_Agile_backend.ai.text.service;

import com.example.KTB_Agile_backend.auth.service.Hashing;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.ai.text.dto.request.ModerationCheckRequest;
import com.example.KTB_Agile_backend.ai.text.dto.response.ModerationCheckResponse;
import com.example.KTB_Agile_backend.ai.text.entity.ModerationCheck;
import com.example.KTB_Agile_backend.ai.text.repository.ModerationCheckRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
public class ModerationCheckService {

	private static final Duration CHECK_TTL = Duration.ofMinutes(5);
	private static final int EXPECTED_CONSUMED_ROWS = 1;

	private final ModerationCheckRepository moderationCheckRepository;
	private final RestClient restClient;
	private final String aiEndpoint;
	private final SecureRandom secureRandom = new SecureRandom();

	public ModerationCheckService(
			ModerationCheckRepository moderationCheckRepository,
			@Value("${ai.text-moderation-url:}") String aiEndpoint,
			@Value("${ai.connect-timeout-seconds:5}") long connectTimeoutSeconds,
			@Value("${ai.read-timeout-seconds:90}") long readTimeoutSeconds
	) {
		this.moderationCheckRepository = moderationCheckRepository;
		this.aiEndpoint = aiEndpoint;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
		requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	public ModerationCheckResponse check(Long userId, ModerationCheckRequest request) {
		if (aiEndpoint.isBlank()) {
			throw aiFailure(null);
		}

		AiModerationResponse aiResponse;
		try {
			aiResponse = restClient.post()
					.uri(aiEndpoint)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("title", request.title(), "content", request.content()))
					.retrieve()
					.body(AiModerationResponse.class);
		} catch (RestClientException exception) {
			throw aiFailure(exception);
		}
		if (aiResponse == null || aiResponse.isAppropriate() == null) {
			throw aiFailure(null);
		}
		if (!aiResponse.isAppropriate()) {
			if (aiResponse.rejectionReason() == null || aiResponse.rejectionReason().isBlank()) {
				throw aiFailure(null);
			}
			return new ModerationCheckResponse(false, aiResponse.rejectionReason(), null);
		}

		byte[] checkIdBytes = new byte[32];
		secureRandom.nextBytes(checkIdBytes);
		String checkId = Base64.getUrlEncoder().withoutPadding().encodeToString(checkIdBytes);
		LocalDateTime now = LocalDateTime.now();
		moderationCheckRepository.save(new ModerationCheck(
				Hashing.sha256(checkId),
				userId,
				contentHash(request.title(), request.content()),
				now.plus(CHECK_TTL)
		));
		return new ModerationCheckResponse(true, aiResponse.rejectionReason(), checkId);
	}

	@Transactional
	public void consumeForItem(Long userId, String checkId, String title, String content) {
		LocalDateTime now = LocalDateTime.now();
		int consumed = moderationCheckRepository.consumeIfValid(
				Hashing.sha256(checkId), userId, contentHash(title, content), now);
		if (consumed != EXPECTED_CONSUMED_ROWS) {
			throw new ApiException(ErrorCode.CONFLICT, "검수 ID가 유효하지 않거나 만료되었습니다.");
		}
	}

	private static String contentHash(String title, String content) {
		return Hashing.sha256(title.length() + ":" + title + content);
	}

	private static ApiException aiFailure(Throwable cause) {
		return new ApiException(ErrorCode.AI_TEXT_MODERATION_FAILED,
				ErrorCode.AI_TEXT_MODERATION_FAILED.message(), cause);
	}

	public record AiModerationResponse(Boolean isAppropriate, String rejectionReason, String keyword) {
	}
}

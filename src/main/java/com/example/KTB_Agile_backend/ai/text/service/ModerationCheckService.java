package com.example.KTB_Agile_backend.ai.text.service;

import com.example.KTB_Agile_backend.ai.config.AiEndpoints;
import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.ai.text.dto.request.ModerationCheckRequest;
import com.example.KTB_Agile_backend.ai.text.dto.response.ModerationCheckResponse;
import com.example.KTB_Agile_backend.ai.text.entity.ModerationCheck;
import com.example.KTB_Agile_backend.ai.text.repository.ModerationCheckRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.util.Hashing;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModerationCheckService {

	private static final Duration CHECK_TTL = Duration.ofMinutes(5);
	private static final int EXPECTED_CONSUMED_ROWS = 1;

	private final ModerationCheckRepository moderationCheckRepository;
	private final AiProperties aiProperties;
	private final RestClient restClient;
	private final SecureRandom secureRandom = new SecureRandom();

	public ModerationCheckResponse check(Long userId, ModerationCheckRequest request) {
		if (aiProperties.baseUrl().isBlank()) {
			throw aiFailure(null);
		}

		AiModerationResponse aiResponse;
		try {
			aiResponse = restClient.post()
					.uri(AiEndpoints.TEXT_MODERATION)
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
		String keyword = aiResponse.keyword() == null ? null : aiResponse.keyword().strip();
		if (keyword != null && keyword.length() > 255) {
			throw aiFailure(null);
		}

		byte[] checkIdBytes = new byte[32];
		secureRandom.nextBytes(checkIdBytes);
		String checkId = Base64.getUrlEncoder().withoutPadding().encodeToString(checkIdBytes);
		LocalDateTime now = LocalDateTime.now();
		moderationCheckRepository.save(new ModerationCheck(
				Hashing.sha256(checkId),
				userId,
				contentHash(request.title(), request.content()),
				keyword,
				now.plus(CHECK_TTL)
		));
		return new ModerationCheckResponse(true, aiResponse.rejectionReason(), checkId);
	}

	@Transactional
	public String consumeForItem(Long userId, String checkId, String title, String content) {
		LocalDateTime now = LocalDateTime.now();
		String checkIdHash = Hashing.sha256(checkId);
		int consumed = moderationCheckRepository.consumeIfValid(
				checkIdHash, userId, contentHash(title, content), now);
		if (consumed != EXPECTED_CONSUMED_ROWS) {
			throw new ApiException(AiErrorCode.AI_TEXT_MODERATION_CHECK_INVALID);
		}
		return moderationCheckRepository.findByCheckIdHash(checkIdHash)
				.orElseThrow(() -> new ApiException(AiErrorCode.AI_TEXT_MODERATION_CHECK_INVALID))
				.getKeyword();
	}

	private static String contentHash(String title, String content) {
		return Hashing.sha256(title.length() + ":" + title + content);
	}

	private static ApiException aiFailure(Throwable cause) {
		return new ApiException(AiErrorCode.AI_TEXT_MODERATION_FAILED, List.of(), cause);
	}

	public record AiModerationResponse(Boolean isAppropriate, String rejectionReason, String keyword) {
	}
}

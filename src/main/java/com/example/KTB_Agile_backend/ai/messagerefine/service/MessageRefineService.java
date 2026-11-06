package com.example.KTB_Agile_backend.ai.messagerefine.service;

import com.example.KTB_Agile_backend.ai.config.AiEndpoints;
import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.ai.messagerefine.dto.ai.MessageRefineAiRequest;
import com.example.KTB_Agile_backend.ai.messagerefine.dto.request.MessageRefineRequest;
import com.example.KTB_Agile_backend.ai.messagerefine.dto.response.MessageRefineResponse;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceAnswerOption;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceQuestion;
import com.example.KTB_Agile_backend.user.dto.response.UserPreferenceResponse;
import com.example.KTB_Agile_backend.user.exception.UserErrorCode;
import com.example.KTB_Agile_backend.user.service.UserPreferenceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MessageRefineService {

	private static final Set<UserPreferenceAnswerOption> CONVERSATION_STYLES = Set.of(
			UserPreferenceAnswerOption.CONCISE,
			UserPreferenceAnswerOption.COMFORTABLE,
			UserPreferenceAnswerOption.WARM
	);
	private static final Set<UserPreferenceAnswerOption> DESCRIPTION_STYLES = Set.of(
			UserPreferenceAnswerOption.BRIEF,
			UserPreferenceAnswerOption.MODERATE,
			UserPreferenceAnswerOption.DETAILED
	);
	private static final Set<UserPreferenceAnswerOption> OPINION_STYLES = Set.of(
			UserPreferenceAnswerOption.CLEAR,
			UserPreferenceAnswerOption.NATURAL,
			UserPreferenceAnswerOption.INDIRECT
	);

	private final AiProperties aiProperties;
	private final RestClient restClient;
	private final UserPreferenceQueryService userPreferenceQueryService;

	public MessageRefineResponse refine(Long userId, MessageRefineRequest request) {
		MessageRefineAiRequest aiRequest = new MessageRefineAiRequest(request.message(), styles(userId));
		if (aiProperties.baseUrl().isBlank()) {
			throw new ApiException(AiErrorCode.AI_MESSAGE_REFINE_FAILED);
		}

		MessageRefineResponse response;
		try {
			response = restClient.post()
					.uri(AiEndpoints.MESSAGE_REFINE)
					.contentType(MediaType.APPLICATION_JSON)
					.body(aiRequest)
					.retrieve()
					.body(MessageRefineResponse.class);
		} catch (RestClientException exception) {
			throw new ApiException(AiErrorCode.AI_MESSAGE_REFINE_FAILED,
					AiErrorCode.AI_MESSAGE_REFINE_FAILED.message(), exception);
		}

		if (response == null || response.refinedMessage() == null || response.refinedMessage().isBlank()) {
			throw new ApiException(AiErrorCode.AI_MESSAGE_REFINE_FAILED);
		}
		return response;
	}

	private MessageRefineAiRequest.Styles styles(Long userId) {
		UserPreferenceResponse preference = userPreferenceQueryService.get(userId);
		Map<UserPreferenceQuestion, UserPreferenceAnswerOption> answers = new EnumMap<>(UserPreferenceQuestion.class);
		for (UserPreferenceResponse.Answer answer : preference.answers()) {
			if (answers.putIfAbsent(answer.question(), answer.answer()) != null) {
				throw missingPreference();
			}
		}

		UserPreferenceAnswerOption conversationStyle = answers.get(UserPreferenceQuestion.CONVERSATION_STYLE);
		UserPreferenceAnswerOption descriptionStyle = answers.get(UserPreferenceQuestion.DESCRIPTION_STYLE);
		UserPreferenceAnswerOption opinionStyle = answers.get(UserPreferenceQuestion.OPINION_STYLE);
		if (answers.size() != UserPreferenceQuestion.values().length
				|| conversationStyle == null
				|| descriptionStyle == null
				|| opinionStyle == null
				|| !CONVERSATION_STYLES.contains(conversationStyle)
				|| !DESCRIPTION_STYLES.contains(descriptionStyle)
				|| !OPINION_STYLES.contains(opinionStyle)) {
			throw missingPreference();
		}
		return new MessageRefineAiRequest.Styles(conversationStyle, descriptionStyle, opinionStyle);
	}

	private static ApiException missingPreference() {
		return new ApiException(UserErrorCode.USER_PREFERENCE_NOT_FOUND);
	}
}

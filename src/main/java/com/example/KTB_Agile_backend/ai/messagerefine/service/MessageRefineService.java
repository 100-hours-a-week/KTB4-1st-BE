package com.example.KTB_Agile_backend.ai.messagerefine.service;

import com.example.KTB_Agile_backend.ai.config.AiEndpoints;
import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.ai.messagerefine.dto.ai.MessageRefineAiRequest;
import com.example.KTB_Agile_backend.ai.messagerefine.dto.ai.MessageRefineAiResponse;
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
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class MessageRefineService {

	private static final int MAX_MESSAGE_CODE_POINTS = 500;
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
	private final ObjectMapper objectMapper;

	public MessageRefineResponse refine(Long userId, MessageRefineRequest request) {
		String message = request == null ? null : request.message();
		if (message == null || message.isBlank()) {
			throw new ApiException(AiErrorCode.MESSAGE_REFINE_EMPTY_MESSAGE);
		}
		if (message.codePointCount(0, message.length()) > MAX_MESSAGE_CODE_POINTS) {
			throw new ApiException(AiErrorCode.MESSAGE_REFINE_MESSAGE_TOO_LONG);
		}

		MessageRefineAiRequest aiRequest = new MessageRefineAiRequest(message, answers(userId));
		if (aiProperties.baseUrl().isBlank()) {
			throw new ApiException(AiErrorCode.MESSAGE_REFINE_SERVER_UNAVAILABLE);
		}

		MessageRefineAiResponse response;
		try {
			response = restClient.post()
					.uri(AiEndpoints.MESSAGE_REFINE)
					.contentType(MediaType.APPLICATION_JSON)
					.body(aiRequest)
					.retrieve()
					.body(MessageRefineAiResponse.class);
		} catch (RestClientResponseException exception) {
			String aiCode = null;
			String aiMessage = null;
			try {
				JsonNode body = objectMapper.readTree(exception.getResponseBodyAsString());
				JsonNode error = body.get("error");
				if (error != null && error.isString()) {
					aiCode = error.stringValue();
				}
				JsonNode aiMessageNode = body.get("message");
				if (aiMessageNode != null && aiMessageNode.isString() && !aiMessageNode.stringValue().isBlank()) {
					aiMessage = aiMessageNode.stringValue();
				}
			} catch (JacksonException ignored) {
				// Use the status code when the AI error body is not JSON.
			}

			AiErrorCode code = AiErrorCode.fromMessageRefineAiCode(
					aiCode, exception.getStatusCode().value());
			throw new ApiException(code, aiMessage == null ? code.message() : aiMessage, exception);
		} catch (RestClientException exception) {
			AiErrorCode code = isTimeout(exception)
					? AiErrorCode.MESSAGE_REFINE_SERVER_TIMEOUT
					: AiErrorCode.MESSAGE_REFINE_SERVER_UNAVAILABLE;
			throw new ApiException(code, code.message(), exception);
		}

		if (response == null || response.refined() == null || response.refined().isBlank()) {
			throw new ApiException(AiErrorCode.INVALID_MESSAGE_REFINE_RESPONSE);
		}
		return new MessageRefineResponse(response.refined());
	}

	private static boolean isTimeout(Throwable exception) {
		Set<Throwable> causes = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Throwable cause = exception; cause != null && causes.add(cause); cause = cause.getCause()) {
			if (cause instanceof SocketTimeoutException
					|| cause instanceof HttpTimeoutException
					|| cause instanceof TimeoutException) {
				return true;
			}
		}
		return false;
	}

	private List<MessageRefineAiRequest.Answer> answers(Long userId) {
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
		return List.of(
				new MessageRefineAiRequest.Answer(
						UserPreferenceQuestion.CONVERSATION_STYLE.name(), conversationStyle.name()),
				new MessageRefineAiRequest.Answer(
						UserPreferenceQuestion.DESCRIPTION_STYLE.name(), descriptionStyle.name()),
				new MessageRefineAiRequest.Answer(
						UserPreferenceQuestion.OPINION_STYLE.name(), opinionStyle.name())
		);
	}

	private static ApiException missingPreference() {
		return new ApiException(UserErrorCode.USER_PREFERENCE_NOT_FOUND);
	}
}

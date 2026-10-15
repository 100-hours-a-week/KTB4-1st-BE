package com.example.KTB_Agile_backend.ai.chatbot.service;

import com.example.KTB_Agile_backend.ai.config.AiEndpoints;
import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.ai.chatbot.dto.ai.ChatbotAiRequest;
import com.example.KTB_Agile_backend.ai.chatbot.dto.ai.ChatbotAiResponse;
import com.example.KTB_Agile_backend.ai.chatbot.dto.request.ChatbotAskRequest;
import com.example.KTB_Agile_backend.ai.chatbot.dto.response.ChatbotAskResponse;
import com.example.KTB_Agile_backend.ai.chatbot.exception.ChatbotErrorCode;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ChatbotService {

	private final AiProperties aiProperties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public ChatbotService(
			ObjectMapper objectMapper,
			AiProperties aiProperties,
			RestClient restClient
	) {
		this.objectMapper = objectMapper;
		this.aiProperties = aiProperties;
		this.restClient = restClient;
	}

	public ChatbotAskResponse ask(ChatbotAskRequest request) {
		if (aiProperties.baseUrl().isBlank()) {
			throw new ApiException(ChatbotErrorCode.AI_CHATBOT_ENDPOINT_NOT_CONFIGURED);
		}

		ChatbotAiRequest aiRequest = new ChatbotAiRequest(
				request.message(),
				request.history().stream()
						.map(message -> new ChatbotAiRequest.HistoryMessage(message.role(), message.content()))
						.toList()
		);

		ChatbotAiResponse aiResponse;
		try {
			aiResponse = restClient.post()
					.uri(AiEndpoints.CHATBOT_ASK)
					.contentType(MediaType.APPLICATION_JSON)
					.body(aiRequest)
					.retrieve()
					.body(ChatbotAiResponse.class);
		} catch (RestClientResponseException exception) {
			throw aiFailure(exception);
		} catch (RestClientException exception) {
			throw new ApiException(
					ChatbotErrorCode.AI_CHATBOT_FAILED,
					ChatbotErrorCode.AI_CHATBOT_FAILED.message(),
					exception
			);
		}

		if (aiResponse == null || aiResponse.answer() == null) {
			throw new ApiException(ChatbotErrorCode.AI_CHATBOT_FAILED);
		}

		ChatbotAskResponse.Action action = null;
		if (aiResponse.action() != null) {
			ChatbotAskResponse.Screen screen = ChatbotAskResponse.Screen.fromValue(aiResponse.action().screen());
			if (screen != null) {
				if (aiResponse.action().label() == null) {
					throw new ApiException(ChatbotErrorCode.AI_CHATBOT_FAILED);
				}
				action = new ChatbotAskResponse.Action(aiResponse.action().label(), screen);
			}
		}
		return new ChatbotAskResponse(aiResponse.answer(), action);
	}

	private ApiException aiFailure(RestClientResponseException exception) {
		ChatbotErrorCode code = ChatbotErrorCode.AI_CHATBOT_FAILED;
		String message = null;
		try {
			JsonNode body = objectMapper.readTree(exception.getResponseBodyAsString());
			JsonNode error = body.get("error");
			if (error != null && error.isString()) {
				code = ChatbotErrorCode.fromAiCode(error.stringValue());
			}
			JsonNode aiMessage = body.get("message");
			if (aiMessage != null && aiMessage.isString() && !aiMessage.stringValue().isBlank()) {
				message = aiMessage.stringValue();
			}
		} catch (JacksonException ignored) {
			// Use the standard error when the AI body is not JSON.
		}
		return new ApiException(code, message == null ? code.message() : message, exception);
	}
}

package com.example.KTB_Agile_backend.ai.chatbot.service;

import com.example.KTB_Agile_backend.ai.chatbot.dto.ai.ChatbotAiRequest;
import com.example.KTB_Agile_backend.ai.chatbot.dto.ai.ChatbotAiResponse;
import com.example.KTB_Agile_backend.ai.chatbot.dto.request.ChatbotAskRequest;
import com.example.KTB_Agile_backend.ai.chatbot.dto.response.ChatbotAskResponse;
import com.example.KTB_Agile_backend.ai.chatbot.exception.ChatbotErrorCode;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
public class ChatbotService {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String aiEndpoint;
	private final ChatbotHistoryService chatbotHistoryService;

	public ChatbotService(
			ObjectMapper objectMapper,
			ChatbotHistoryService chatbotHistoryService,
			@Value("${ai.chatbot-url:}") String aiEndpoint,
			@Value("${ai.connect-timeout-seconds:5}") long connectTimeoutSeconds,
			@Value("${ai.read-timeout-seconds:90}") long readTimeoutSeconds
	) {
		this.objectMapper = objectMapper;
		this.chatbotHistoryService = chatbotHistoryService;
		this.aiEndpoint = aiEndpoint;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
		requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	public ChatbotAskResponse ask(Long userId, ChatbotAskRequest request) {
		if (aiEndpoint.isBlank()) {
			throw new ApiException(ChatbotErrorCode.AI_CHATBOT_ENDPOINT_NOT_CONFIGURED);
		}

		ChatbotAiRequest aiRequest = new ChatbotAiRequest(
				request.message(),
				chatbotHistoryService.findRecentHistory(userId)
		);

		ChatbotAiResponse aiResponse;
		try {
			aiResponse = restClient.post()
					.uri(aiEndpoint)
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
		chatbotHistoryService.saveExchange(userId, request.message(), aiResponse.answer());
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

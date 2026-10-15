package com.example.KTB_Agile_backend.ai.chatbot.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum ChatbotErrorCode implements ApiErrorCode {
	AI_CHATBOT_ENDPOINT_NOT_CONFIGURED(
			"AI_CHATBOT_ENDPOINT_NOT_CONFIGURED", "AI 챗봇 서버 주소가 설정되지 않았습니다."),
	AI_CHATBOT_FAILED(
			"AI_CHATBOT_FAILED", "챗봇 요청 처리에 실패했어요. 잠시 후 다시 시도해주세요."),
	CLAUDE_RATE_LIMITED(
			"claude_rate_limited", "요청 한도를 초과했어요. 잠시 후 다시 시도해주세요."),
	CLAUDE_TIMEOUT(
			"claude_timeout", "응답이 지연되고 있어요. 잠시 후 다시 시도해주세요."),
	CLAUDE_API_ERROR(
			"claude_api_error", "AI 응답 처리에 실패했어요. 잠시 후 다시 시도해주세요."),
	INVALID_MODEL_RESPONSE(
			"invalid_model_response", "AI 응답을 해석하지 못했어요. 잠시 후 다시 시도해주세요."),
	EMBEDDING_MODEL_ERROR(
			"embedding_model_error", "정책 검색 준비에 실패했어요. 잠시 후 다시 시도해주세요."),
	RETRIEVAL_ERROR(
			"retrieval_error", "정책 검색에 실패했어요. 잠시 후 다시 시도해주세요.");

	private final String value;
	private final String message;

	ChatbotErrorCode(String value, String message) {
		this.value = value;
		this.message = message;
	}

	public static ChatbotErrorCode fromAiCode(String value) {
		if (value == null) {
			return AI_CHATBOT_FAILED;
		}
		return switch (value) {
			case "claude_rate_limited" -> CLAUDE_RATE_LIMITED;
			case "claude_timeout" -> CLAUDE_TIMEOUT;
			case "claude_api_error" -> CLAUDE_API_ERROR;
			case "invalid_model_response" -> INVALID_MODEL_RESPONSE;
			case "embedding_model_error" -> EMBEDDING_MODEL_ERROR;
			case "retrieval_error" -> RETRIEVAL_ERROR;
			default -> AI_CHATBOT_FAILED;
		};
	}

	@Override
	public String value() {
		return value;
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	@Override
	public String message() {
		return message;
	}
}

package com.example.KTB_Agile_backend.ai.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

public enum AiErrorCode implements ApiErrorCode {
	AI_ANALYSIS_FAILED("AI_ANALYSIS_FAILED", HttpStatus.BAD_GATEWAY, "AI 이미지 분석 서버 호출에 실패했습니다."),
	AI_ANALYSIS_ENDPOINT_NOT_CONFIGURED("AI_ANALYSIS_ENDPOINT_NOT_CONFIGURED", HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 서버 주소가 설정되지 않았습니다."),
	AI_TEXT_MODERATION_FAILED("AI_TEXT_MODERATION_FAILED", HttpStatus.BAD_GATEWAY, "AI 텍스트 검수 서버 호출에 실패했습니다."),
	AI_TEXT_MODERATION_CHECK_INVALID("AI_TEXT_MODERATION_CHECK_INVALID", HttpStatus.CONFLICT, "검수 ID가 유효하지 않거나 만료되었습니다."),
	AI_ITEM_PRICE_ENDPOINT_NOT_CONFIGURED("AI_ITEM_PRICE_ENDPOINT_NOT_CONFIGURED", HttpStatus.INTERNAL_SERVER_ERROR, "AI 가격 측정 서버 주소가 설정되지 않았습니다."),
	AI_ITEM_PRICE_ESTIMATION_FAILED("AI_ITEM_PRICE_ESTIMATION_FAILED", HttpStatus.BAD_GATEWAY, "AI 가격 측정 서버 호출에 실패했습니다."),
	AI_SMART_SEARCH_ENDPOINT_NOT_CONFIGURED("AI_SMART_SEARCH_ENDPOINT_NOT_CONFIGURED", HttpStatus.INTERNAL_SERVER_ERROR, "AI 스마트 검색 서버 주소가 설정되지 않았습니다."),
	AI_SMART_SEARCH_FAILED("AI_SMART_SEARCH_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "스마트 검색에 실패했어요. 잠시 후 다시 시도해주세요."),
	AI_MESSAGE_REFINE_FAILED("AI_MESSAGE_REFINE_FAILED", HttpStatus.BAD_GATEWAY, "AI 메시지 다듬기에 실패했습니다."),
	MESSAGE_REFINE_EMPTY_MESSAGE("empty_message", HttpStatus.BAD_REQUEST, "다듬을 문장을 입력해주세요."),
	MESSAGE_REFINE_MESSAGE_TOO_LONG("message_too_long", HttpStatus.BAD_REQUEST, "문장은 500자 이하로 입력해주세요."),
	MESSAGE_REFINE_UNREFINABLE("unrefinable_message", HttpStatus.BAD_REQUEST, "다듬을 수 있는 문장을 입력해주세요."),
	MESSAGE_REFINE_SERVER_UNAVAILABLE("refine_server_unavailable", HttpStatus.BAD_GATEWAY, "다듬기 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."),
	MESSAGE_REFINE_SERVER_TIMEOUT("refine_server_timeout", HttpStatus.BAD_GATEWAY, "다듬기 서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."),
	MESSAGE_REFINE_SERVER_AUTH_ERROR("refine_server_auth_error", HttpStatus.BAD_GATEWAY, "다듬기 서버 인증에 실패했습니다."),
	MESSAGE_REFINE_SERVER_OVERLOADED("refine_server_overloaded", HttpStatus.BAD_GATEWAY, "다듬기 서버가 과부하 상태입니다. 잠시 후 다시 시도해주세요."),
	MESSAGE_REFINE_SERVER_API_ERROR("refine_server_api_error", HttpStatus.BAD_GATEWAY, "다듬기 서버 내부 오류가 발생했습니다."),
	INVALID_MESSAGE_REFINE_RESPONSE("invalid_refine_response", HttpStatus.BAD_GATEWAY, "다듬기 서버 응답 형식이 올바르지 않습니다.");

	private final String codeValue;
	private final HttpStatus httpStatus;
	private final String errorMessage;
	private static final Map<String, AiErrorCode> MESSAGE_REFINE_CODES = Map.of(
			MESSAGE_REFINE_EMPTY_MESSAGE.codeValue, MESSAGE_REFINE_EMPTY_MESSAGE,
			MESSAGE_REFINE_MESSAGE_TOO_LONG.codeValue, MESSAGE_REFINE_MESSAGE_TOO_LONG,
			MESSAGE_REFINE_UNREFINABLE.codeValue, MESSAGE_REFINE_UNREFINABLE,
			MESSAGE_REFINE_SERVER_UNAVAILABLE.codeValue, MESSAGE_REFINE_SERVER_UNAVAILABLE,
			MESSAGE_REFINE_SERVER_TIMEOUT.codeValue, MESSAGE_REFINE_SERVER_TIMEOUT,
			MESSAGE_REFINE_SERVER_AUTH_ERROR.codeValue, MESSAGE_REFINE_SERVER_AUTH_ERROR,
			MESSAGE_REFINE_SERVER_OVERLOADED.codeValue, MESSAGE_REFINE_SERVER_OVERLOADED,
			MESSAGE_REFINE_SERVER_API_ERROR.codeValue, MESSAGE_REFINE_SERVER_API_ERROR,
			INVALID_MESSAGE_REFINE_RESPONSE.codeValue, INVALID_MESSAGE_REFINE_RESPONSE
	);

	AiErrorCode(String value, HttpStatus status, String message) {
		this.codeValue = value;
		this.httpStatus = status;
		this.errorMessage = message;
	}

	public static AiErrorCode fromMessageRefineAiCode(String value, int status) {
		AiErrorCode code = value == null ? null : MESSAGE_REFINE_CODES.get(value);
		return code == null ? fromMessageRefineHttpStatus(status) : code;
	}

	private static AiErrorCode fromMessageRefineHttpStatus(int status) {
		if (status == 401 || status == 403) {
			return MESSAGE_REFINE_SERVER_AUTH_ERROR;
		}
		if (status == 408 || status == 504) {
			return MESSAGE_REFINE_SERVER_TIMEOUT;
		}
		if (status == 429 || status == 503) {
			return MESSAGE_REFINE_SERVER_OVERLOADED;
		}
		return status >= 500 ? MESSAGE_REFINE_SERVER_API_ERROR : AI_MESSAGE_REFINE_FAILED;
	}

	@Override
	public String value() {return codeValue;}

	@Override
	public HttpStatus status() {
		return httpStatus;
	}

	@Override
	public String message() {
		return errorMessage;
	}
}

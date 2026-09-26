package com.example.KTB_Agile_backend.ai.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum AiErrorCode implements ApiErrorCode {
	AI_ANALYSIS_FAILED("AI_ANALYSIS_FAILED", HttpStatus.BAD_GATEWAY, "AI 이미지 분석 서버 호출에 실패했습니다."),
	AI_ANALYSIS_ENDPOINT_NOT_CONFIGURED("AI_ANALYSIS_ENDPOINT_NOT_CONFIGURED", HttpStatus.INTERNAL_SERVER_ERROR,
			"AI 분석 서버 주소가 설정되지 않았습니다."),
	AI_TEXT_MODERATION_FAILED("AI_TEXT_MODERATION_FAILED", HttpStatus.BAD_GATEWAY, "AI 텍스트 검수 서버 호출에 실패했습니다."),
	AI_TEXT_MODERATION_CHECK_INVALID("AI_TEXT_MODERATION_CHECK_INVALID", HttpStatus.CONFLICT,
			"검수 ID가 유효하지 않거나 만료되었습니다.");

	private final String codeValue;
	private final HttpStatus httpStatus;
	private final String errorMessage;

	AiErrorCode(String value, HttpStatus status, String message) {
		this.codeValue = value;
		this.httpStatus = status;
		this.errorMessage = message;
	}

	@Override
	public String value() {
		return codeValue;
	}

	@Override
	public HttpStatus status() {
		return httpStatus;
	}

	@Override
	public String message() {
		return errorMessage;
	}
}

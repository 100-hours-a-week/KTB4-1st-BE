package com.example.KTB_Agile_backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode implements ApiErrorCode {
	BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	REQUEST_VALIDATION_FAILED("REQUEST_VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	REQUEST_BODY_INVALID("REQUEST_BODY_INVALID", HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다."),
	AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED,
			"로그인이 필요하거나 Access Token이 만료되었거나 유효하지 않습니다."),
	CONFLICT("CONFLICT", HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
	UNPROCESSABLE_ENTITY("UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_CONTENT, "요청을 처리할 수 없습니다."),
	NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
	INVALID_CURSOR("INVALID_CURSOR", HttpStatus.BAD_REQUEST, "cursor가 올바르지 않습니다.");

	private final String codeValue;
	private final HttpStatus httpStatus;
	private final String errorMessage;

	ErrorCode(String value, HttpStatus status, String message) {
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

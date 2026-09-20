package com.example.KTB_Agile_backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	REQUEST_VALIDATION_FAILED("REQUEST_VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	REQUEST_BODY_INVALID("REQUEST_BODY_INVALID", HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다."),
	AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	AUTHENTICATION_FAILED("AUTHENTICATION_FAILED", HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
	AUTH_OAUTH_STATE_INVALID("AUTH_OAUTH_STATE_INVALID", HttpStatus.UNAUTHORIZED, "OAuth 인증 상태가 유효하지 않습니다."),
	AUTH_OAUTH_CODE_INVALID("AUTH_OAUTH_CODE_INVALID", HttpStatus.BAD_REQUEST, "인가 코드가 유효하지 않습니다."),
	AUTH_OAUTH_AUTHENTICATION_FAILED(
			"AUTH_OAUTH_AUTHENTICATION_FAILED",
			HttpStatus.UNAUTHORIZED,
			"OAuth 인증에 실패했습니다."
	),
	AUTH_PROVIDER_REQUIRED("AUTH_PROVIDER_REQUIRED", HttpStatus.BAD_REQUEST, "provider는 필수 입력값입니다."),
	AUTH_PROVIDER_UNSUPPORTED("AUTH_PROVIDER_UNSUPPORTED", HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth provider입니다."),
	AUTH_REFRESH_TOKEN_INVALID(
				"AUTH_REFRESH_TOKEN_INVALID",
				HttpStatus.UNAUTHORIZED,
				"Refresh Token이 만료되었거나 유효하지 않습니다."
		),
	CONFLICT("CONFLICT", HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
	NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
	SOCIAL_ACCOUNT_CONFLICT("SOCIAL_ACCOUNT_CONFLICT", HttpStatus.CONFLICT, "계정 연결 정보가 충돌했습니다."),
	USER_PREFERENCE_INVALID("USER_PREFERENCE_INVALID", HttpStatus.BAD_REQUEST, "질문 3개를 모두 입력해야 합니다."),
	USER_PREFERENCE_ALREADY_EXISTS(
			"USER_PREFERENCE_ALREADY_EXISTS",
			HttpStatus.CONFLICT,
			"사용자 설명은 회원가입 시 한 번만 설정할 수 있습니다."
	),
	USER_PREFERENCE_NOT_FOUND("USER_PREFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자 취향이 설정되지 않았습니다.");

	private final String value;
	private final HttpStatus status;
	private final String message;

	ErrorCode(String value, HttpStatus status, String message) {
		this.value = value;
		this.status = status;
		this.message = message;
	}

	public String value() {
		return value;
	}

	public HttpStatus status() {
		return status;
	}

	public String message() {
		return message;
	}
}

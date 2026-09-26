package com.example.KTB_Agile_backend.auth.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ApiErrorCode {
	AUTHENTICATION_FAILED("AUTHENTICATION_FAILED", HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
	AUTH_OAUTH_STATE_INVALID("AUTH_OAUTH_STATE_INVALID", HttpStatus.UNAUTHORIZED, "OAuth 인증 상태가 유효하지 않습니다."),
	AUTH_OAUTH_CODE_INVALID("AUTH_OAUTH_CODE_INVALID", HttpStatus.BAD_REQUEST, "인가 코드가 유효하지 않습니다."),
	AUTH_OAUTH_AUTHENTICATION_FAILED("AUTH_OAUTH_AUTHENTICATION_FAILED", HttpStatus.UNAUTHORIZED, "OAuth 인증에 실패했습니다."),
	AUTH_PROVIDER_REQUIRED("AUTH_PROVIDER_REQUIRED", HttpStatus.BAD_REQUEST, "provider는 필수 입력값입니다."),
	AUTH_PROVIDER_UNSUPPORTED("AUTH_PROVIDER_UNSUPPORTED", HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth provider입니다."),
	AUTH_REFRESH_TOKEN_INVALID("AUTH_REFRESH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED,
			"Refresh Token이 만료되었거나 유효하지 않습니다.");

	private final String codeValue;
	private final HttpStatus httpStatus;
	private final String errorMessage;

	AuthErrorCode(String value, HttpStatus status, String message) {
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

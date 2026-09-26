package com.example.KTB_Agile_backend.user.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ApiErrorCode {
	SOCIAL_ACCOUNT_CONFLICT("SOCIAL_ACCOUNT_CONFLICT", HttpStatus.CONFLICT, "계정 연결 정보가 충돌했습니다."),
	USER_PREFERENCE_INVALID("USER_PREFERENCE_INVALID", HttpStatus.BAD_REQUEST, "질문 3개를 모두 입력해야 합니다."),
	USER_PREFERENCE_ALREADY_EXISTS("USER_PREFERENCE_ALREADY_EXISTS", HttpStatus.CONFLICT,
			"사용자 설명은 회원가입 시 한 번만 설정할 수 있습니다."),
	USER_PREFERENCE_NOT_FOUND("USER_PREFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자 취향이 설정되지 않았습니다.");

	private final String value;
	private final HttpStatus status;
	private final String message;

	UserErrorCode(String value, HttpStatus status, String message) {
		this.value = value;
		this.status = status;
		this.message = message;
	}

	@Override
	public String value() {
		return value;
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String message() {
		return message;
	}
}

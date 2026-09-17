package com.example.KTB_Agile_backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST),
	UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
	FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
	SOCIAL_ACCOUNT_CONFLICT("SOCIAL_ACCOUNT_CONFLICT", HttpStatus.CONFLICT);

	private final String value;
	private final HttpStatus status;

	ErrorCode(String value, HttpStatus status) {
		this.value = value;
		this.status = status;
	}

	public String value() {
		return value;
	}

	public HttpStatus status() {
		return status;
	}
}

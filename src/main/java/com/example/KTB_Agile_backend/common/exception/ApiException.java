package com.example.KTB_Agile_backend.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

	private final ErrorCode code;
	private final List<ErrorDetail> details;

	public ApiException(ErrorCode code) {
		this(code, code.message());
	}

	public ApiException(ErrorCode code, String message) {
		this(code, message, List.of());
	}

	public ApiException(
			ErrorCode code,
			String message,
			List<ErrorDetail> details
	) {
		this(code, message, details, null);
	}

	public ApiException(
			ErrorCode code,
			String message,
			List<ErrorDetail> details,
			Throwable cause
	) {
		super(message, cause);
		this.code = code;
		this.details = details == null ? List.of() : List.copyOf(details);
	}

	public ApiException(ErrorCode code, List<ErrorDetail> details) {
		this(code, code.message(), details);
	}

	public ApiException(ErrorCode code, List<ErrorDetail> details, Throwable cause) {
		this(code, code.message(), details, cause);
	}

	public HttpStatus status() {
		return code.status();
	}

	public ErrorCode code() {
		return code;
	}

	public List<ErrorDetail> details() {
		return details;
	}
}

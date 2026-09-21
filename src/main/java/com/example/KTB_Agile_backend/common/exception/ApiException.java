package com.example.KTB_Agile_backend.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final ErrorCode errorCode;
	private final List<ErrorDetail> errorDetails;

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
		this.errorCode = code;
		this.errorDetails = details == null ? List.of() : List.copyOf(details);
	}

	public ApiException(ErrorCode code, List<ErrorDetail> details) {
		this(code, code.message(), details);
	}

	public ApiException(ErrorCode code, List<ErrorDetail> details, Throwable cause) {
		this(code, code.message(), details, cause);
	}

	public HttpStatus status() {
		return errorCode.status();
	}

	public ErrorCode code() {
		return errorCode;
	}

	public List<ErrorDetail> details() {
		return errorDetails;
	}
}

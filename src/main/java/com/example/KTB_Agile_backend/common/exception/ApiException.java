package com.example.KTB_Agile_backend.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final ApiErrorCode errorCode;
	private final List<ErrorDetail> errorDetails;

	public ApiException(ApiErrorCode code) {
		this(code, code.message());
	}

	public ApiException(ApiErrorCode code, String message) {
		this(code, message, List.of());
	}

	public ApiException(ApiErrorCode code, String message, Throwable cause) {
		this(code, message, List.of(), cause);
	}

	public ApiException(
			ApiErrorCode code,
			String message,
			List<ErrorDetail> details
	) {
		this(code, message, details, null);
	}

	public ApiException(
			ApiErrorCode code,
			String message,
			List<ErrorDetail> details,
			Throwable cause
	) {
		super(message, cause);
		this.errorCode = code;
		this.errorDetails = details == null ? List.of() : List.copyOf(details);
	}

	public ApiException(ApiErrorCode code, List<ErrorDetail> details) {
		this(code, code.message(), details);
	}

	public ApiException(ApiErrorCode code, List<ErrorDetail> details, Throwable cause) {
		this(code, code.message(), details, cause);
	}

	public HttpStatus status() {
		return errorCode.status();
	}

	public ApiErrorCode code() {
		return errorCode;
	}

	public List<ErrorDetail> details() {
		return errorDetails;
	}
}

package com.example.KTB_Agile_backend.common.exception;

import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final ErrorResponse error;

	public ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, List.of());
	}

	public ApiException(
			HttpStatus status,
			String code,
			String message,
			List<ErrorResponse.Field> details
	) {
		this(status, code, message, details, null);
	}

	public ApiException(
			HttpStatus status,
			String code,
			String message,
			List<ErrorResponse.Field> details,
			Throwable cause
	) {
		super(message, cause);
		this.status = status;
		this.error = new ErrorResponse(code, message, details == null ? List.of() : List.copyOf(details));
	}

	public HttpStatus status() {
		return status;
	}

	public ErrorResponse error() {
		return error;
	}
}

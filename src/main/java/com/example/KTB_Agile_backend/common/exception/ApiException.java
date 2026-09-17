package com.example.KTB_Agile_backend.common.exception;

import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

	private final ErrorCode code;
	private final ErrorResponse error;

	public ApiException(ErrorCode code, String message) {
		this(code, message, List.of());
	}

	public ApiException(
			ErrorCode code,
			String message,
			List<ErrorResponse.Field> details
	) {
		this(code, message, details, null);
	}

	public ApiException(
			ErrorCode code,
			String message,
			List<ErrorResponse.Field> details,
			Throwable cause
	) {
		super(message, cause);
		this.code = code;
		this.error = new ErrorResponse(code.value(), message, details == null ? List.of() : List.copyOf(details));
	}

	public HttpStatus status() {
		return code.status();
	}

	public ErrorResponse error() {
		return error;
	}
}

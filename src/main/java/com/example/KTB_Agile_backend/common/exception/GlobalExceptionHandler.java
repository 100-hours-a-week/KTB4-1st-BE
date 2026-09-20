package com.example.KTB_Agile_backend.common.exception;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
		ErrorResponse error = new ErrorResponse(
				exception.code().value(),
				exception.getMessage(),
				exception.details().stream()
						.map(detail -> new ErrorResponse.Field(detail.field(), detail.reason()))
						.toList()
		);
		return response(exception.status(), error);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
		List<ErrorResponse.Field> details = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.Field(
						error.getField(),
						error.getDefaultMessage() == null ? "유효하지 않은 입력값입니다." : error.getDefaultMessage()
				))
				.toList();
		return response(ErrorCode.REQUEST_VALIDATION_FAILED.status(),
				new ErrorResponse(
						ErrorCode.REQUEST_VALIDATION_FAILED.value(),
						ErrorCode.REQUEST_VALIDATION_FAILED.message(),
						details
				));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
		return response(ErrorCode.REQUEST_BODY_INVALID.status(),
				new ErrorResponse(ErrorCode.REQUEST_BODY_INVALID.value(), ErrorCode.REQUEST_BODY_INVALID.message(), List.of()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch() {
		return response(ErrorCode.BAD_REQUEST.status(),
				new ErrorResponse(ErrorCode.BAD_REQUEST.value(), ErrorCode.BAD_REQUEST.message(), List.of()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request
	) {
		log.error("Unhandled API exception: {} {}", request.getMethod(), request.getRequestURI(), exception);
		return response(ErrorCode.INTERNAL_SERVER_ERROR.status(),
				new ErrorResponse(
						ErrorCode.INTERNAL_SERVER_ERROR.value(),
						ErrorCode.INTERNAL_SERVER_ERROR.message(),
						List.of()
				));
	}

	private static ResponseEntity<ApiResponse<Void>> response(
			HttpStatus status,
			ErrorResponse error
	) {
		return ResponseEntity.status(status).body(new ApiResponse<>(null, error));
	}

}

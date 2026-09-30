package com.example.KTB_Agile_backend.common.exception;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
		return response(ErrorCode.REQUEST_BODY_INVALID);
	}

	@ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
	public ResponseEntity<ApiResponse<Void>> handleInvalidParameter() {
		return response(ErrorCode.BAD_REQUEST);
	}

	@ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
	public ResponseEntity<ApiResponse<Void>> handleNotFound() {
		return response(ErrorCode.NOT_FOUND);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
			HttpRequestMethodNotSupportedException exception
	) {
		return response(ErrorCode.METHOD_NOT_ALLOWED, exception.getHeaders());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
			HttpMediaTypeNotSupportedException exception
	) {
		return response(ErrorCode.UNSUPPORTED_MEDIA_TYPE, exception.getHeaders());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request
	) {
		if (log.isErrorEnabled()) {
			log.error("Unhandled API exception: {} {}", request.getMethod(), request.getRequestURI(), exception);
		}
		return response(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	private static ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode) {
		return response(errorCode, HttpHeaders.EMPTY);
	}

	private static ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode, HttpHeaders headers) {
		return ResponseEntity.status(errorCode.status())
				.headers(responseHeaders -> responseHeaders.addAll(headers))
				.body(new ApiResponse<>(null,
						new ErrorResponse(errorCode.value(), errorCode.message(), List.of())));
	}

	private static ResponseEntity<ApiResponse<Void>> response(
			HttpStatus status,
			ErrorResponse error
	) {
		return ResponseEntity.status(status).body(new ApiResponse<>(null, error));
	}
}

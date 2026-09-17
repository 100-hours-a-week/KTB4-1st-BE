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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
		return response(exception.status(), exception.error());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
		List<ErrorResponse.Field> details = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.Field(
						error.getField(),
						error.getDefaultMessage() == null ? "유효하지 않은 입력값입니다." : error.getDefaultMessage()
				))
				.toList();
		String message = details.stream().anyMatch(detail -> "authorizationCode".equals(detail.field()))
				? "인가 코드가 유효하지 않습니다."
				: "요청 값이 올바르지 않습니다.";
		return response(ErrorCode.BAD_REQUEST.status(),
				new ErrorResponse(ErrorCode.BAD_REQUEST.value(), message, details));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
		return response(ErrorCode.BAD_REQUEST.status(),
				new ErrorResponse(ErrorCode.BAD_REQUEST.value(), "요청 본문 형식이 올바르지 않습니다.", List.of()));
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
						internalMessage(request.getRequestURI()),
						List.of()
				));
	}

	private static ResponseEntity<ApiResponse<Void>> response(
			HttpStatus status,
			ErrorResponse error
	) {
		return ResponseEntity.status(status).body(new ApiResponse<>(null, error));
	}

	private static String internalMessage(String requestUri) {
		return switch (requestUri) {
			case "/auth/oauth" -> "인증 처리 중 서버 오류가 발생했습니다.";
			case "/auth/refresh" -> "토큰 재발급 중 서버 오류가 발생했습니다.";
			case "/auth/logout" -> "로그아웃 처리 중 서버 오류가 발생했습니다.";
			default -> "서버 오류가 발생했습니다.";
		};
	}
}

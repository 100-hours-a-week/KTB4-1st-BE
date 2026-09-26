package com.example.KTB_Agile_backend.image.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum ImageErrorCode implements ApiErrorCode {
	IMAGE_NOT_PENDING("IMAGE_NOT_PENDING", HttpStatus.CONFLICT, "미등록 상태의 이미지가 아닙니다."),
	IMAGE_NOT_OWNED("IMAGE_NOT_OWNED", HttpStatus.FORBIDDEN, "소유하지 않은 이미지입니다."),
	S3_BUCKET_NOT_CONFIGURED("S3_BUCKET_NOT_CONFIGURED", HttpStatus.INTERNAL_SERVER_ERROR, "S3 버킷이 설정되지 않았습니다."),
	S3_IMAGE_NOT_FOUND("S3_IMAGE_NOT_FOUND", HttpStatus.NOT_FOUND, "S3에서 이미지를 찾을 수 없습니다."),
	S3_IMAGE_PROCESSING_FAILED("S3_IMAGE_PROCESSING_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
			"S3 이미지 처리에 실패했습니다."),
	UNSUPPORTED_IMAGE_TYPE("UNSUPPORTED_IMAGE_TYPE", HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.");

	private final String value;
	private final HttpStatus status;
	private final String message;

	ImageErrorCode(String value, HttpStatus status, String message) {
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

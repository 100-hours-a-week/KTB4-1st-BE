package com.example.KTB_Agile_backend.item.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum ItemErrorCode implements ApiErrorCode {
	ITEM_IMAGE_ALREADY_REGISTERED("ITEM_IMAGE_ALREADY_REGISTERED", HttpStatus.CONFLICT, "이미 등록된 이미지입니다."),
	ITEM_NOT_FOUND("ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "물품을 찾을 수 없습니다."),
	ITEM_UPDATE_FORBIDDEN("ITEM_UPDATE_FORBIDDEN", HttpStatus.FORBIDDEN, "물품을 수정할 권한이 없습니다."),
	ITEM_REGISTRATION_GROUP_NOT_FOUND("ITEM_REGISTRATION_GROUP_NOT_FOUND", HttpStatus.NOT_FOUND,
			"등록할 그룹을 찾을 수 없습니다."),
	ITEM_REGISTRATION_GROUP_MEMBERSHIP_REQUIRED("ITEM_REGISTRATION_GROUP_MEMBERSHIP_REQUIRED", HttpStatus.FORBIDDEN,
			"모든 그룹의 ACTIVE 멤버만 물품을 등록할 수 있습니다."),
	ITEM_UPDATE_IMAGE_NOT_FOUND("ITEM_UPDATE_IMAGE_NOT_FOUND", HttpStatus.NOT_FOUND, "수정할 이미지를 찾을 수 없습니다."),
	ITEM_UPDATE_IMAGE_NOT_OWNED("ITEM_UPDATE_IMAGE_NOT_OWNED", HttpStatus.FORBIDDEN,
			"소유하지 않은 이미지는 수정할 수 없습니다."),
	ITEM_UPDATE_IMAGE_CONFLICT("ITEM_UPDATE_IMAGE_CONFLICT", HttpStatus.CONFLICT, "이미 다른 물품에 연결된 이미지입니다.");

	private final String codeValue;
	private final HttpStatus httpStatus;
	private final String errorMessage;

	ItemErrorCode(String value, HttpStatus status, String message) {
		this.codeValue = value;
		this.httpStatus = status;
		this.errorMessage = message;
	}

	@Override
	public String value() {
		return codeValue;
	}

	@Override
	public HttpStatus status() {
		return httpStatus;
	}

	@Override
	public String message() {
		return errorMessage;
	}
}

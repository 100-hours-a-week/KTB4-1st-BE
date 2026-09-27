package com.example.KTB_Agile_backend.exchange.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum ExchangeErrorCode implements ApiErrorCode {
	EXCHANGE_REQUEST_CREATE_INVALID("EXCHANGE_REQUEST_CREATE_INVALID", HttpStatus.BAD_REQUEST,
			"교환 요청의 itemId, requestedQuantity 또는 offeredItems 형식이 올바르지 않습니다."),
	EXCHANGE_REQUEST_CREATE_FORBIDDEN("EXCHANGE_REQUEST_CREATE_FORBIDDEN", HttpStatus.FORBIDDEN,
			"해당 물품에 교환 요청을 보낼 권한이 없습니다."),
	EXCHANGE_REQUEST_CREATE_NOT_FOUND("EXCHANGE_REQUEST_CREATE_NOT_FOUND", HttpStatus.NOT_FOUND,
			"대상 물품 또는 제안 물품을 찾을 수 없습니다."),
	EXCHANGE_REQUEST_CREATE_CONFLICT("EXCHANGE_REQUEST_CREATE_CONFLICT", HttpStatus.CONFLICT,
			"이미 처리 중인 교환 요청이 있거나 거래 가능한 물품이 아닙니다."),
	EXCHANGE_REQUEST_CREATE_QUANTITY_EXCEEDED("EXCHANGE_REQUEST_CREATE_QUANTITY_EXCEEDED", HttpStatus.UNPROCESSABLE_CONTENT,
			"요청 수량 또는 제안 수량이 보유 수량을 초과했습니다."),
	EXCHANGE_REQUEST_UPDATE_INVALID("EXCHANGE_REQUEST_UPDATE_INVALID", HttpStatus.BAD_REQUEST,
			"수정할 교환 요청의 requestedQuantity 또는 offeredItems 형식이 올바르지 않습니다."),
	EXCHANGE_REQUEST_UPDATE_FORBIDDEN("EXCHANGE_REQUEST_UPDATE_FORBIDDEN", HttpStatus.FORBIDDEN,
			"교환 요청을 수정할 권한이 없습니다."),
	EXCHANGE_REQUEST_UPDATE_NOT_FOUND("EXCHANGE_REQUEST_UPDATE_NOT_FOUND", HttpStatus.NOT_FOUND,
			"교환 요청 또는 대상 물품을 찾을 수 없습니다."),
	EXCHANGE_REQUEST_UPDATE_CONFLICT("EXCHANGE_REQUEST_UPDATE_CONFLICT", HttpStatus.CONFLICT,
			"교환 요청이 수정 가능한 상태가 아니거나 대상 물품을 거래할 수 없습니다."),
	EXCHANGE_REQUEST_UPDATE_QUANTITY_EXCEEDED("EXCHANGE_REQUEST_UPDATE_QUANTITY_EXCEEDED", HttpStatus.UNPROCESSABLE_CONTENT,
			"요청 수량 또는 제안 수량이 보유 수량을 초과했습니다."),
	EXCHANGE_REQUEST_STATUS_INVALID("EXCHANGE_REQUEST_STATUS_INVALID", HttpStatus.BAD_REQUEST,
			"교환 요청 ID 또는 상태(status)가 올바르지 않습니다."),
	EXCHANGE_REQUEST_STATUS_FORBIDDEN("EXCHANGE_REQUEST_STATUS_FORBIDDEN", HttpStatus.FORBIDDEN,
			"교환 요청 상태를 변경할 권한이 없습니다."),
	EXCHANGE_REQUEST_STATUS_NOT_FOUND("EXCHANGE_REQUEST_STATUS_NOT_FOUND", HttpStatus.NOT_FOUND, "교환 요청을 찾을 수 없습니다."),
	EXCHANGE_REQUEST_STATUS_CONFLICT("EXCHANGE_REQUEST_STATUS_CONFLICT", HttpStatus.CONFLICT,
			"이미 처리되었거나 현재 상태에서는 변경할 수 없습니다.");

	private final String codeValue;
	private final HttpStatus httpStatus;
	private final String errorMessage;

	ExchangeErrorCode(String value, HttpStatus status, String message) {
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

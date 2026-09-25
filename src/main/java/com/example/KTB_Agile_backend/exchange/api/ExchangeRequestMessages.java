package com.example.KTB_Agile_backend.exchange.api;

public final class ExchangeRequestMessages {

	public static final String CREATE_BAD_REQUEST =
			"교환 요청의 itemId, requestedQuantity 또는 offeredItems 형식이 올바르지 않습니다.";
	public static final String CREATE_FORBIDDEN = "해당 물품에 교환 요청을 보낼 권한이 없습니다.";
	public static final String CREATE_NOT_FOUND = "대상 물품 또는 제안 물품을 찾을 수 없습니다.";
	public static final String CREATE_CONFLICT = "이미 처리 중인 교환 요청이 있거나 거래 가능한 물품이 아닙니다.";
	public static final String CREATE_INSUFFICIENT_QUANTITY = "요청 수량 또는 제안 수량이 보유 수량을 초과했습니다.";
	public static final String CREATE_INTERNAL_ERROR = "교환 요청 처리 중 서버 오류가 발생했습니다.";
	public static final String STATUS_BAD_REQUEST = "교환 요청 ID 또는 상태(status)가 올바르지 않습니다.";
	public static final String STATUS_FORBIDDEN = "교환 요청 상태를 변경할 권한이 없습니다.";
	public static final String STATUS_NOT_FOUND = "교환 요청을 찾을 수 없습니다.";
	public static final String STATUS_CONFLICT = "이미 처리되었거나 현재 상태에서는 변경할 수 없습니다.";
	public static final String STATUS_INTERNAL_ERROR = "교환 요청 상태 변경 중 서버 오류가 발생했습니다.";
	public static final String INVALID_STATUS_REASON = "COMPLETED 또는 REJECTED만 입력해 주세요.";

	private ExchangeRequestMessages() {
	}

	public static boolean isExchangePath(String path) {
		return path != null && (path.contains("/exchange-requests/") || path.endsWith("/exchange-requests"));
	}

	public static boolean isCreatePath(String path) {
		return path != null && path.endsWith("/exchange-requests");
	}

	public static String badRequestMessage(String path) {
		return isCreatePath(path) ? CREATE_BAD_REQUEST : STATUS_BAD_REQUEST;
	}

	public static String internalErrorMessage(String path) {
		return isCreatePath(path) ? CREATE_INTERNAL_ERROR : STATUS_INTERNAL_ERROR;
	}
}

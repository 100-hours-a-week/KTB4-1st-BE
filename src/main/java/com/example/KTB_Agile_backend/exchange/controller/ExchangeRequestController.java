package com.example.KTB_Agile_backend.exchange.controller;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import com.example.KTB_Agile_backend.common.exception.ErrorDetail;
import com.example.KTB_Agile_backend.exchange.exception.ExchangeErrorCode;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.exchange.dto.request.ExchangeRequestCreateRequest;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestCreatedResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestStatusResponse;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.service.ExchangeRequestService;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExchangeRequestController {

	private final ExchangeRequestService exchangeRequestService;

	@PostMapping("/api/items/{itemId}/exchange-requests")
	public ResponseEntity<ApiResponse<ExchangeRequestCreatedResponse>> create(
			Authentication authentication,
			@PathVariable String itemId,
			@RequestBody JsonNode body
	) {
		long targetItemId = parseId(itemId, ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_INVALID);
		ExchangeRequestCreateRequest request = parseCreateRequest(body);
		ExchangeRequestCreatedResponse response = exchangeRequestService.create(
				Long.valueOf(authentication.getName()), targetItemId, request);
		return ResponseEntity.created(URI.create("/api/exchange-requests/" + response.exchangeRequestId()))
				.body(new ApiResponse<>(response, null));
	}

	@PatchMapping("/api/exchange-requests/{exchangeRequestId}/status")
	public ResponseEntity<ApiResponse<ExchangeRequestStatusResponse>> updateStatus(
			Authentication authentication,
			@PathVariable String exchangeRequestId,
			@RequestBody JsonNode body
	) {
		long requestId = parseId(exchangeRequestId, ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_INVALID);
		ExchangeRequestStatus status = parseStatus(body);
		ExchangeRequestStatusResponse response = exchangeRequestService.updateStatus(
				requestId, Long.valueOf(authentication.getName()), status);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	private static ExchangeRequestCreateRequest parseCreateRequest(JsonNode body) {
		if (body == null || !body.isObject()) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_INVALID);
		}
		JsonNode requestedQuantity = body.get("requestedQuantity");
		JsonNode offeredItems = body.get("offeredItems");
		if (!isPositiveInteger(requestedQuantity) || offeredItems == null || !offeredItems.isArray()
				|| offeredItems.size() == 0) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_INVALID);
		}

		List<ExchangeRequestCreateRequest.OfferedItemRequest> items = new ArrayList<>();
		for (JsonNode offered : offeredItems) {
			if (!offered.isObject() || !isPositiveLong(offered.get("itemId"))
					|| !isPositiveInteger(offered.get("quantity"))) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_INVALID);
			}
			items.add(new ExchangeRequestCreateRequest.OfferedItemRequest(
					offered.get("itemId").longValue(), offered.get("quantity").intValue()));
		}
		return new ExchangeRequestCreateRequest(requestedQuantity.intValue(), items);
	}

	private static ExchangeRequestStatus parseStatus(JsonNode body) {
		JsonNode status = body != null && body.isObject() ? body.get("status") : null;
		if (status != null && status.isString()) {
			if (ExchangeRequestStatus.COMPLETED.name().equals(status.stringValue())) {
				return ExchangeRequestStatus.COMPLETED;
			}
			if (ExchangeRequestStatus.REJECTED.name().equals(status.stringValue())) {
				return ExchangeRequestStatus.REJECTED;
			}
		}
		throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_INVALID,
				List.of(new ErrorDetail("status", "COMPLETED 또는 REJECTED만 입력해 주세요.")));
	}

	private static boolean isPositiveInteger(JsonNode value) {
		return value != null && value.isIntegralNumber() && value.canConvertToInt() && value.intValue() > 0;
	}

	private static boolean isPositiveLong(JsonNode value) {
		return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0;
	}

	private static long parseId(String value, ApiErrorCode errorCode) {
		try {
			long id = Long.parseLong(value);
			if (id > 0) {
				return id;
			}
		} catch (NumberFormatException ignored) {
			// Return the endpoint-specific bad request below.
		}
		throw new ApiException(errorCode);
	}
}

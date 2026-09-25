package com.example.KTB_Agile_backend.exchange.controller;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.exception.ErrorDetail;
import com.example.KTB_Agile_backend.common.response.ApiResponse;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestCreatedResponse;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestCreateRequest;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestMessages;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestStatusResponse;
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
		long targetItemId = parseId(itemId, ExchangeRequestMessages.CREATE_BAD_REQUEST);
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
		long requestId = parseId(exchangeRequestId, ExchangeRequestMessages.STATUS_BAD_REQUEST);
		ExchangeRequestStatus status = parseStatus(body);
		ExchangeRequestStatusResponse response = exchangeRequestService.updateStatus(
				requestId, Long.valueOf(authentication.getName()), status);
		return ResponseEntity.ok(new ApiResponse<>(response, null));
	}

	private static ExchangeRequestCreateRequest parseCreateRequest(JsonNode body) {
		if (body == null || !body.isObject()) {
			throw badRequest(ExchangeRequestMessages.CREATE_BAD_REQUEST);
		}
		JsonNode requestedQuantity = body.get("requestedQuantity");
		JsonNode offeredItems = body.get("offeredItems");
		if (!isPositiveInteger(requestedQuantity) || offeredItems == null || !offeredItems.isArray()
				|| offeredItems.size() == 0) {
			throw badRequest(ExchangeRequestMessages.CREATE_BAD_REQUEST);
		}

		List<ExchangeRequestCreateRequest.OfferedItemRequest> items = new ArrayList<>();
		for (JsonNode offered : offeredItems) {
			if (!offered.isObject() || !isPositiveLong(offered.get("itemId"))
					|| !isPositiveInteger(offered.get("quantity"))) {
				throw badRequest(ExchangeRequestMessages.CREATE_BAD_REQUEST);
			}
			items.add(new ExchangeRequestCreateRequest.OfferedItemRequest(
					offered.get("itemId").longValue(), offered.get("quantity").intValue()));
		}
		return new ExchangeRequestCreateRequest(requestedQuantity.intValue(), items);
	}

	private static ExchangeRequestStatus parseStatus(JsonNode body) {
		JsonNode status = body != null && body.isObject() ? body.get("status") : null;
		if (status != null && status.isString()) {
			if ("COMPLETED".equals(status.stringValue())) {
				return ExchangeRequestStatus.COMPLETED;
			}
			if ("REJECTED".equals(status.stringValue())) {
				return ExchangeRequestStatus.REJECTED;
			}
		}
		throw badRequest(ExchangeRequestMessages.STATUS_BAD_REQUEST,
				List.of(new ErrorDetail("status", ExchangeRequestMessages.INVALID_STATUS_REASON)));
	}

	private static boolean isPositiveInteger(JsonNode value) {
		return value != null && value.isIntegralNumber() && value.canConvertToInt() && value.intValue() > 0;
	}

	private static boolean isPositiveLong(JsonNode value) {
		return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0;
	}

	private static long parseId(String value, String message) {
		try {
			long id = Long.parseLong(value);
			if (id > 0) {
				return id;
			}
		} catch (NumberFormatException ignored) {
			// Return the endpoint-specific bad request below.
		}
		throw badRequest(message);
	}

	private static ApiException badRequest(String message) {
		return badRequest(message, List.of());
	}

	private static ApiException badRequest(String message, List<ErrorDetail> details) {
		return new ApiException(ErrorCode.BAD_REQUEST, message, details);
	}
}

package com.example.KTB_Agile_backend.exchange.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.exchange.exception.ExchangeErrorCode;
import com.example.KTB_Agile_backend.exchange.dto.request.ExchangeRequestCreateRequest;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestCreatedResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestCreatedResponse.OfferedItemResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestStatusResponse;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.entity.OfferedItem;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExchangeRequestService {

	private static final ZoneOffset API_OFFSET = ZoneOffset.ofHours(9);

	private final ExchangeRequestRepository exchangeRequestRepository;
	private final ItemRepository itemRepository;
	private final UserRepository userRepository;

	@Transactional
	public ExchangeRequestCreatedResponse create(
			Long requesterId,
			Long itemId,
			ExchangeRequestCreateRequest request
	) {
		validate(request, itemId);
		User requester = userRepository.findActiveById(requesterId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));

		List<Long> itemIds = new ArrayList<>();
		itemIds.add(itemId);
		request.offeredItems().forEach(offered -> itemIds.add(offered.itemId()));
		Map<Long, Item> items = lockItems(itemIds);
		if (items.size() != itemIds.size() || items.values().stream().anyMatch(Item::isDeleted)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_NOT_FOUND);
		}

		Item requestedItem = items.get(itemId);
		if (requestedItem.getUser().getId().equals(requesterId)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_FORBIDDEN);
		}
		if (requestedItem.getItemState() != ItemState.AVAILABLE) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_CONFLICT);
		}
		checkQuantity(requestedItem, request.requestedQuantity());

		List<Item> offered = new ArrayList<>();
		for (ExchangeRequestCreateRequest.OfferedItemRequest offeredRequest : request.offeredItems()) {
			Item offeredItem = items.get(offeredRequest.itemId());
			if (!offeredItem.getUser().getId().equals(requesterId)) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_FORBIDDEN);
			}
			if (offeredItem.getItemState() != ItemState.AVAILABLE) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_CONFLICT);
			}
			checkQuantity(offeredItem, offeredRequest.quantity());
			offered.add(offeredItem);
		}

		if (!exchangeRequestRepository.findByRequesterAndItemAndStatusForUpdate(
				requesterId, itemId, ExchangeRequestStatus.PENDING).isEmpty()) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_CONFLICT);
		}

		ExchangeRequest exchangeRequest = new ExchangeRequest(requester, requestedItem, request.requestedQuantity());
		for (int index = 0; index < offered.size(); index++) {
			exchangeRequest.addOfferedItem(offered.get(index), request.offeredItems().get(index).quantity());
		}
		exchangeRequestRepository.saveAndFlush(exchangeRequest);

		return new ExchangeRequestCreatedResponse(
				exchangeRequest.getId(), itemId, request.requestedQuantity(),
				request.offeredItems().stream()
						.map(item -> new OfferedItemResponse(item.itemId(), item.quantity())).toList(),
				exchangeRequest.getRequestedStatus(), null, toOffsetDateTime(exchangeRequest.getCreatedAt())
		);
	}

	@Transactional
	public ExchangeRequestStatusResponse updateStatus(
			Long exchangeRequestId,
			Long userId,
			ExchangeRequestStatus status
	) {
		Map<Long, Item> items = Map.of();
		if (status == ExchangeRequestStatus.COMPLETED) {
			Long itemId = exchangeRequestRepository.findItemIdById(exchangeRequestId)
					.orElseThrow(() -> new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_NOT_FOUND));
			List<Long> itemIds = new ArrayList<>();
			itemIds.add(itemId);
			itemIds.addAll(exchangeRequestRepository.findOfferedItemIdsByRequestId(exchangeRequestId));
			items = lockItems(itemIds);
			if (items.size() != itemIds.size()) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_NOT_FOUND);
			}
		}
		ExchangeRequest exchangeRequest = exchangeRequestRepository.findByIdForUpdate(exchangeRequestId)
				.orElseThrow(() -> new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_NOT_FOUND));
		if (!exchangeRequest.getItem().getUser().getId().equals(userId)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_FORBIDDEN);
		}
		if (exchangeRequest.getRequestedStatus() != ExchangeRequestStatus.PENDING) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_CONFLICT);
		}

		if (status == ExchangeRequestStatus.COMPLETED) {
			items.get(exchangeRequest.getItem().getId())
					.deductForCompletedExchange(exchangeRequest.getRequestedQuantity());
			for (OfferedItem offered : exchangeRequest.getOfferedItems()) {
				items.get(offered.getItem().getId()).deductForCompletedExchange(offered.getQuantity());
			}
		}

		exchangeRequest.changeStatus(status);
		exchangeRequestRepository.saveAndFlush(exchangeRequest);
		return new ExchangeRequestStatusResponse(
				exchangeRequest.getId(), exchangeRequest.getItem().getId(), status,
				toOffsetDateTime(exchangeRequest.getUpdatedAt())
		);
	}

	private Map<Long, Item> lockItems(List<Long> itemIds) {
		Map<Long, Item> items = new HashMap<>();
		itemRepository.findAllForExchange(itemIds).forEach(item -> items.put(item.getId(), item));
		return items;
	}

	private static void validate(ExchangeRequestCreateRequest request, Long itemId) {
		if (itemId == null || itemId < 1 || request == null || request.requestedQuantity() == null
				|| request.requestedQuantity() < 1 || request.offeredItems() == null
				|| request.offeredItems().isEmpty()) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_INVALID);
		}
		Set<Long> offeredIds = new HashSet<>();
		for (ExchangeRequestCreateRequest.OfferedItemRequest offered : request.offeredItems()) {
			if (offered == null || offered.itemId() == null || offered.itemId() < 1 || offered.quantity() == null
					|| offered.quantity() < 1 || offered.itemId().equals(itemId)
					|| !offeredIds.add(offered.itemId())) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_INVALID);
			}
		}
	}

	private static void checkQuantity(Item item, int requestedQuantity) {
		if (item.getQuantity() < requestedQuantity) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_QUANTITY_EXCEEDED);
		}
	}

	private static OffsetDateTime toOffsetDateTime(LocalDateTime timestamp) {
		return timestamp == null ? null : timestamp.atOffset(API_OFFSET);
	}
}

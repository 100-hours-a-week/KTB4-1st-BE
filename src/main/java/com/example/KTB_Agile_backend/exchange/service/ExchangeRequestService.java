package com.example.KTB_Agile_backend.exchange.service;

import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import com.example.KTB_Agile_backend.chat.entity.ChatMemberRole;
import com.example.KTB_Agile_backend.chat.entity.ChatRoom;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatRoomRepository;
import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
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
	private final ChatMemberRepository chatMemberRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ItemRepository itemRepository;
	private final UserRepository userRepository;

	@Transactional
	public ExchangeRequestCreatedResponse create(
			Long requesterId,
			Long itemId,
			ExchangeRequestCreateRequest request
	) {
		validate(request, itemId, ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_INVALID);
		User requester = userRepository.findActiveById(requesterId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));

		List<Long> itemIds = new ArrayList<>();
		itemIds.add(itemId);
		request.offeredItems().forEach(offered -> itemIds.add(offered.itemId()));
		Map<Long, Item> items = lockItems(itemIds);
		if (items.size() != itemIds.size() || items.values().stream().anyMatch(Item::isDeleted)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_NOT_FOUND);
		}

		validateItems(requesterId, itemId, request, items,
				ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_FORBIDDEN,
				ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_CONFLICT,
				ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_QUANTITY_EXCEEDED);

		Item requestedItem = items.get(itemId);
		if (!exchangeRequestRepository.findByRequesterAndItemAndStatus(
				requesterId, itemId, ExchangeRequestStatus.PENDING).isEmpty()) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_CONFLICT);
		}

		ExchangeRequest exchangeRequest = new ExchangeRequest(requester, requestedItem, request.requestedQuantity());
		for (ExchangeRequestCreateRequest.OfferedItemRequest offeredRequest : request.offeredItems()) {
			exchangeRequest.addOfferedItem(items.get(offeredRequest.itemId()), offeredRequest.quantity());
		}
		exchangeRequestRepository.saveAndFlush(exchangeRequest);
		ChatRoom chatRoom = chatRoomRepository.saveAndFlush(new ChatRoom(exchangeRequest));
		chatMemberRepository.saveAll(List.of(
				new ChatMember(chatRoom, requester, ChatMemberRole.REQUESTER),
				new ChatMember(chatRoom, requestedItem.getUser(), ChatMemberRole.OWNER)
		));

		return new ExchangeRequestCreatedResponse(
				exchangeRequest.getId(), itemId, request.requestedQuantity(),
				request.offeredItems().stream()
						.map(item -> new OfferedItemResponse(item.itemId(), item.quantity())).toList(),
				exchangeRequest.getRequestedStatus(), chatRoom.getId(), toOffsetDateTime(exchangeRequest.getCreatedAt())
		);
	}

	@Transactional
	public void update(Long exchangeRequestId, Long requesterId, ExchangeRequestCreateRequest request) {
		ExchangeRequest exchangeRequest = exchangeRequestRepository.findByIdForUpdate(exchangeRequestId)
				.orElseThrow(() -> new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_NOT_FOUND));
		if (!exchangeRequest.getRequester().getId().equals(requesterId)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_FORBIDDEN);
		}
		if (exchangeRequest.getRequestedStatus() != ExchangeRequestStatus.PENDING) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_CONFLICT);
		}
		Long itemId = exchangeRequest.getItem().getId();
		validate(request, itemId, ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_INVALID);

		List<Long> itemIds = new ArrayList<>();
		itemIds.add(itemId);
		request.offeredItems().forEach(offered -> itemIds.add(offered.itemId()));
		Map<Long, Item> items = lockItems(itemIds);
		if (items.size() != itemIds.size() || items.values().stream().anyMatch(Item::isDeleted)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_NOT_FOUND);
		}
		validateItems(requesterId, itemId, request, items,
				ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_FORBIDDEN,
				ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_CONFLICT,
				ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_QUANTITY_EXCEEDED);

		exchangeRequest.updateRequestedQuantity(request.requestedQuantity());
		updateOfferedItems(exchangeRequest, request, items);
		exchangeRequestRepository.saveAndFlush(exchangeRequest);
	}

	@Transactional
	public ExchangeRequestStatusResponse updateStatus(
			Long exchangeRequestId,
			Long userId,
			ExchangeRequestStatus status
	) {
		if (status != ExchangeRequestStatus.COMPLETED
				&& status != ExchangeRequestStatus.REJECTED
				&& status != ExchangeRequestStatus.CANCELED) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_INVALID);
		}
		ExchangeRequest exchangeRequest = exchangeRequestRepository.findByIdForUpdate(exchangeRequestId)
				.orElseThrow(() -> new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_NOT_FOUND));
		boolean authorized = status == ExchangeRequestStatus.CANCELED
				? exchangeRequest.getRequester().getId().equals(userId)
				: exchangeRequest.getItem().getUser().getId().equals(userId);
		if (!authorized) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_FORBIDDEN);
		}
		if (exchangeRequest.getRequestedStatus() != ExchangeRequestStatus.PENDING) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_CONFLICT);
		}

		if (status == ExchangeRequestStatus.COMPLETED) {
			List<Long> itemIds = new ArrayList<>();
			itemIds.add(exchangeRequest.getItem().getId());
			exchangeRequest.getOfferedItems().forEach(offered -> itemIds.add(offered.getItem().getId()));
			Map<Long, Item> items = lockItems(itemIds);
			if (items.size() != itemIds.size()) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_STATUS_NOT_FOUND);
			}
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

	private static void validate(
			ExchangeRequestCreateRequest request,
			Long itemId,
			ApiErrorCode invalidCode
	) {
		if (itemId == null || itemId < 1 || request == null || request.requestedQuantity() == null
				|| request.requestedQuantity() < 1 || request.offeredItems() == null
				|| request.offeredItems().isEmpty()) {
			throw new ApiException(invalidCode);
		}
		Set<Long> offeredIds = new HashSet<>();
		for (ExchangeRequestCreateRequest.OfferedItemRequest offered : request.offeredItems()) {
			if (offered == null || offered.itemId() == null || offered.itemId() < 1 || offered.quantity() == null
					|| offered.quantity() < 1 || offered.itemId().equals(itemId)
					|| !offeredIds.add(offered.itemId())) {
				throw new ApiException(invalidCode);
			}
		}
	}

	private static void validateItems(
			Long requesterId,
			Long itemId,
			ExchangeRequestCreateRequest request,
			Map<Long, Item> items,
			ApiErrorCode forbiddenCode,
			ApiErrorCode conflictCode,
			ApiErrorCode quantityExceededCode
	) {
		Item requestedItem = items.get(itemId);
		if (requestedItem.getUser().getId().equals(requesterId)) {
			throw new ApiException(forbiddenCode);
		}
		if (requestedItem.getItemState() != ItemState.AVAILABLE) {
			throw new ApiException(conflictCode);
		}
		checkQuantity(requestedItem, request.requestedQuantity(), quantityExceededCode);

		for (ExchangeRequestCreateRequest.OfferedItemRequest offeredRequest : request.offeredItems()) {
			Item offeredItem = items.get(offeredRequest.itemId());
			if (!offeredItem.getUser().getId().equals(requesterId)) {
				throw new ApiException(forbiddenCode);
			}
			if (offeredItem.getItemState() != ItemState.AVAILABLE) {
				throw new ApiException(conflictCode);
			}
			checkQuantity(offeredItem, offeredRequest.quantity(), quantityExceededCode);
		}
	}

	private static void updateOfferedItems(
			ExchangeRequest exchangeRequest,
			ExchangeRequestCreateRequest request,
			Map<Long, Item> items
	) {
		Map<Long, Integer> quantities = new HashMap<>();
		request.offeredItems().forEach(offered -> quantities.put(offered.itemId(), offered.quantity()));
		for (OfferedItem offeredItem : new ArrayList<>(exchangeRequest.getOfferedItems())) {
			Integer quantity = quantities.remove(offeredItem.getItem().getId());
			if (quantity == null) {
				exchangeRequest.removeOfferedItem(offeredItem);
			} else {
				offeredItem.changeQuantity(quantity);
			}
		}
		for (ExchangeRequestCreateRequest.OfferedItemRequest offered : request.offeredItems()) {
			Integer quantity = quantities.remove(offered.itemId());
			if (quantity != null) {
				exchangeRequest.addOfferedItem(items.get(offered.itemId()), quantity);
			}
		}
	}

	private static void checkQuantity(Item item, int requestedQuantity, ApiErrorCode errorCode) {
		if (item.getQuantity() < requestedQuantity) {
			throw new ApiException(errorCode);
		}
	}

	private static OffsetDateTime toOffsetDateTime(LocalDateTime timestamp) {
		return timestamp == null ? null : timestamp.atOffset(API_OFFSET);
	}
}

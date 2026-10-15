package com.example.KTB_Agile_backend.search.service;

import com.example.KTB_Agile_backend.ai.config.AiEndpoints;
import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.exception.ExchangeErrorCode;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.exception.GroupErrorCode;
import com.example.KTB_Agile_backend.group.repository.GroupItemRepository;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.service.ItemQueryService;
import com.example.KTB_Agile_backend.search.dto.ai.RankCandidatesRequest;
import com.example.KTB_Agile_backend.search.dto.ai.RankCandidatesResponse;
import com.example.KTB_Agile_backend.search.dto.request.SmartSearchRequest;
import com.example.KTB_Agile_backend.search.dto.response.SmartSearchResponse;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SmartSearchService {

	private static final int MAX_CANDIDATES = 30;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final GroupItemRepository groupItemRepository;
	private final ItemRepository itemRepository;
	private final ItemQueryService itemQueryService;
	private final SmartSearchBudgetCalculator budgetCalculator;
	private final ObjectMapper objectMapper;
	private final AiProperties aiProperties;
	private final RestClient restClient;

	public SmartSearchResponse search(Long userId, SmartSearchRequest request) {
		validateGroupMembership(userId, request.groupId());
		List<Long> selectedIds = request.items().stream().map(SmartSearchRequest.SelectedItem::itemId).toList();
		if (new HashSet<>(selectedIds).size() != selectedIds.size()) {
			throw new ApiException(ErrorCode.BAD_REQUEST, "같은 물품은 한 번만 선택할 수 있습니다.");
		}
		List<Item> selectedItems = itemRepository.findAllByIdInAndDeletedAtIsNull(selectedIds);
		if (selectedItems.size() != request.items().size()) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_NOT_FOUND);
		}

		Map<Long, Integer> quantities = new HashMap<>();
		request.items().forEach(item -> quantities.put(item.itemId(), item.quantity()));
		for (Item item : selectedItems) {
			if (!item.getUser().getId().equals(userId)) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_FORBIDDEN);
			}
			if (item.getItemState() != ItemState.AVAILABLE || item.getQuantity() <= 0) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_CONFLICT);
			}
			int quantity = quantities.get(item.getId());
			if (quantity > item.getQuantity()) {
				throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_QUANTITY_EXCEEDED);
			}
		}
		Set<Long> selectedItemsInGroup = new HashSet<>();
		for (GroupItem groupItem : groupItemRepository.findActiveGroupItemsByItemIds(selectedIds)) {
			if (groupItem.getGroup().getId().equals(request.groupId())) {
				selectedItemsInGroup.add(groupItem.getItem().getId());
			}
		}
		if (!selectedItemsInGroup.containsAll(selectedIds)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_CREATE_FORBIDDEN);
		}

		BigDecimal budget = budgetCalculator.calculateBudget(selectedItems, quantities);
		BigDecimal maximumUnitPrice = budgetCalculator.calculateMaximumUnitPrice(budget);

		List<ItemSummaryProjection> candidates = groupItemRepository.findSmartSearchCandidates(
				request.groupId(), userId, maximumUnitPrice, ItemState.AVAILABLE,
				UserStatus.ACTIVE, GroupMemberStatus.ACTIVE, ExchangeRequestStatus.PENDING,
				PageRequest.of(0, MAX_CANDIDATES));
		if (candidates.isEmpty()) {
			return new SmartSearchResponse(List.of());
		}
		if (request.preferenceText() == null || request.preferenceText().isBlank()) {
			return new SmartSearchResponse(itemQueryService.toSummariesForUser(userId, candidates));
		}

		RankCandidatesResponse response = rankCandidates(candidates, request.preferenceText());
		List<Long> candidateIds = candidates.stream().map(ItemSummaryProjection::itemId).toList();
		List<Long> rankedIds = response == null ? null : response.results();
		if (rankedIds == null || rankedIds.size() != candidateIds.size()
				|| !new HashSet<>(rankedIds).equals(new HashSet<>(candidateIds))) {
			throw smartSearchFailed(null);
		}

		Map<Long, ItemSummaryProjection> candidateById = new HashMap<>();
		candidates.forEach(candidate -> candidateById.put(candidate.itemId(), candidate));
		List<ItemSummaryProjection> rankedCandidates = rankedIds.stream()
				.map(candidateById::get)
				.toList();
		List<ItemSummary> items = itemQueryService.toSummariesForUser(userId, rankedCandidates);
		return new SmartSearchResponse(items);
	}

	private void validateGroupMembership(Long userId, Long groupId) {
		if (!groupRepository.existsByIdAndDeletedAtIsNull(groupId)) {
			throw new ApiException(GroupErrorCode.GROUP_NOT_FOUND);
		}
		GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
				.orElseThrow(() -> new ApiException(GroupErrorCode.GROUP_MEMBERSHIP_REQUIRED));
		if (member.getStatus() != GroupMemberStatus.ACTIVE) {
			throw new ApiException(GroupErrorCode.GROUP_MEMBERSHIP_REQUIRED);
		}
	}

	private RankCandidatesResponse rankCandidates(List<ItemSummaryProjection> candidates, String preferenceText) {
		if (aiProperties.baseUrl().isBlank()) {
			throw new ApiException(AiErrorCode.AI_SMART_SEARCH_ENDPOINT_NOT_CONFIGURED);
		}
		List<RankCandidatesRequest.Candidate> aiCandidates = candidates.stream()
				.map(candidate -> new RankCandidatesRequest.Candidate(candidate.itemId(), candidate.title()))
				.toList();
		try {
			return restClient.post()
					.uri(AiEndpoints.SMART_SEARCH)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new RankCandidatesRequest(aiCandidates, preferenceText))
					.retrieve()
					.body(RankCandidatesResponse.class);
		} catch (RestClientResponseException exception) {
			throw smartSearchFailed(aiErrorMessage(exception), exception);
		} catch (RestClientException exception) {
			throw smartSearchFailed(null, exception);
		}
	}

	private String aiErrorMessage(RestClientResponseException exception) {
		try {
			JsonNode message = objectMapper.readTree(exception.getResponseBodyAsString()).get("message");
			if (message != null && message.isString() && !message.stringValue().isBlank()) {
				return message.stringValue();
			}
		} catch (JacksonException ignored) {
			// Use the standard message when the AI error body is not valid JSON.
		}
		return AiErrorCode.AI_SMART_SEARCH_FAILED.message();
	}

	private static ApiException smartSearchFailed(Throwable cause) {
		return smartSearchFailed(AiErrorCode.AI_SMART_SEARCH_FAILED.message(), cause);
	}

	private static ApiException smartSearchFailed(String message, Throwable cause) {
		return new ApiException(AiErrorCode.AI_SMART_SEARCH_FAILED, message, cause);
	}
}

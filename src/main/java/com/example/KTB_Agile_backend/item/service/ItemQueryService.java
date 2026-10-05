package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.exception.GroupErrorCode;
import com.example.KTB_Agile_backend.group.repository.GroupItemRepository;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.image.repository.ImageRepository;
import com.example.KTB_Agile_backend.image.service.S3ImageObjectService;
import com.example.KTB_Agile_backend.item.dto.response.ItemDetailResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemPageResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.dto.response.MyItemPageResponse;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemLike;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.exception.ItemErrorCode;
import com.example.KTB_Agile_backend.item.repository.ItemLikeRepository;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemQueryService {

	private static final int PAGE_SIZE = 20;
	private static final int FETCH_SIZE = PAGE_SIZE + 1;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String MY_ITEMS_BAD_REQUEST_MESSAGE = "size 또는 cursor 값이 올바르지 않습니다.";
	private static final int CONTENT_PREVIEW_LENGTH = 70;

	private final ItemRepository itemRepository;
	private final ItemStatsRepository itemStatsRepository;
	private final ItemLikeRepository itemLikeRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final GroupItemRepository groupItemRepository;
	private final ImageRepository imageRepository;
	private final S3ImageObjectService s3ImageObjectService;
	private final ExchangeRequestRepository exchangeRequestRepository;

	@Transactional(readOnly = true)
	public ItemDetailResponse findDetail(Long userId, Long itemId) {
		Item item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(ItemErrorCode.ITEM_NOT_FOUND));
		ItemStats stats = itemStatsRepository.findById(itemId).orElse(null);

		return new ItemDetailResponse(
				item.getId(),
				groupItemRepository.findActiveGroupItemsByItemId(itemId).stream()
						.map(groupItem -> new ItemDetailResponse.GroupInfo(
								groupItem.getGroup().getId(),
								groupItem.getGroup().getGroupName()
						))
						.toList(),
				item.getTitle(),
				item.getContent(),
				item.getQuantity(),
				item.getItemState(),
				new ItemDetailResponse.Owner(
						item.getUser().getId(),
						item.getUser().getNickname(),
						item.getUser().getProfileImageUrl()
				),
				toImageInfos(imageRepository.findAllByItem_IdOrderByIdAsc(itemId)),
				stats == null ? 0L : stats.getLikeCount(),
				stats == null ? 0L : stats.getViewCount(),
				0L,
				itemLikeRepository.existsByItem_IdAndUser_Id(itemId, userId),
				item.getCreatedAt(),
				item.getUpdatedAt(),
				item.getExchangeUrgencyScore(),
				item.getValueGapToleranceScore()
		);
	}

	@Transactional(readOnly = true)
	public ItemPageResponse findByGroup(Long userId, Long groupId, String cursor) {
		if (!groupRepository.existsByIdAndDeletedAtIsNull(groupId)) {
			throw new ApiException(GroupErrorCode.GROUP_NOT_FOUND);
		}
		GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
				.orElseThrow(() -> new ApiException(GroupErrorCode.GROUP_MEMBERSHIP_REQUIRED));
		if (member.getStatus() != GroupMemberStatus.ACTIVE) {
			throw new ApiException(GroupErrorCode.GROUP_MEMBERSHIP_REQUIRED);
		}

		Long cursorId = CursorCodec.decodeId(cursor);
		Pageable pageable = PageRequest.of(0, FETCH_SIZE);
		List<ItemSummaryProjection> summaries = cursorId == null
				? groupItemRepository.findActiveItemSummariesByGroupId(groupId, pageable)
				: groupItemRepository.findActiveItemSummariesByGroupIdAfter(groupId, cursorId, pageable);
		CursorPage<ItemSummaryProjection> page = CursorPage.fromIds(
				summaries, PAGE_SIZE, ItemSummaryProjection::itemId);
		List<Long> itemIds = page.items().stream().map(ItemSummaryProjection::itemId).toList();
		Map<Long, Long> likeCounts = findLikeCounts(itemIds);
		Set<Long> likedItemIds = findLikedItemIds(userId, itemIds);
		Map<Long, String> thumbnails = findThumbnails(itemIds);

		return new ItemPageResponse(
				page.items().stream()
						.map(summary -> toSummary(summary, likeCounts, likedItemIds, thumbnails))
						.toList(),
				page.nextCursor(),
				page.hasNext()
		);
	}

	@Transactional(readOnly = true)
	public MyItemPageResponse findMyItems(Long userId, String sizeValue, String cursor) {
		int size = parseMyItemsSize(sizeValue);
		Long cursorId = CursorCodec.decodeId(cursor);
		Pageable pageable = PageRequest.of(0, size + 1);
		List<Item> items = cursorId == null
			? itemRepository.findAllByUser_IdAndDeletedAtIsNullOrderByIdDesc(userId, pageable)
			: itemRepository.findAllByUser_IdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(
						userId, cursorId, pageable);
		CursorPage<Item> page = CursorPage.fromIds(items, size, Item::getId);
		List<Long> itemIds = itemIds(page.items());
		Map<Long, Long> likeCounts = findLikeCounts(itemIds);
		Set<Long> likedItemIds = findLikedItemIds(userId, itemIds);
		Map<Long, String> thumbnails = findThumbnails(itemIds);
		Map<Long, List<MyItemPageResponse.GroupInfo>> groups = findGroupInfos(page.items());
		Map<Long, Long> exchangeRequestCounts = findExchangeRequestCounts(itemIds);

		return new MyItemPageResponse(
				page.items().stream()
						.map(item -> new MyItemPageResponse.MyItem(
								item.getId(),
								groups.getOrDefault(item.getId(), List.of()),
								item.getTitle(),
								contentPreview(item.getContent()),
								item.getQuantity(),
								item.getItemState(),
								thumbnails.get(item.getId()),
								likeCounts.getOrDefault(item.getId(), 0L),
								exchangeRequestCounts.getOrDefault(item.getId(), 0L),
								likedItemIds.contains(item.getId()),
								item.getCreatedAt()
						))
						.toList(),
				page.nextCursor(),
				page.hasNext()
		);
	}

	private static int parseMyItemsSize(String sizeValue) {
		try {
			int size = Integer.parseInt(sizeValue);
			if (size >= 1 && size <= MAX_PAGE_SIZE) {
				return size;
			}
		} catch (NumberFormatException ignored) {
			// Return the endpoint-specific bad request below.
		}
		throw invalidMyItemsRequest();
	}

	private Map<Long, List<MyItemPageResponse.GroupInfo>> findGroupInfos(List<Item> items) {
		if (items.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<MyItemPageResponse.GroupInfo>> groups = new HashMap<>();
		groupItemRepository.findActiveGroupItemsByItemIds(itemIds(items)).forEach(groupItem ->
				groups.computeIfAbsent(groupItem.getItem().getId(), ignored -> new ArrayList<>()).add(
						new MyItemPageResponse.GroupInfo(
								groupItem.getGroup().getId(),
								groupItem.getGroup().getGroupName()
						)
				)
		);
		return groups;
	}

	private Map<Long, Long> findExchangeRequestCounts(List<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Long> counts = new HashMap<>();
		exchangeRequestRepository.findExchangeRequestCountsByItemIds(itemIds)
				.forEach(count -> counts.put(count.getItemId(), count.getExchangeRequestCount()));
		return counts;
	}

	private static ApiException invalidMyItemsRequest() {
		return new ApiException(ErrorCode.BAD_REQUEST, MY_ITEMS_BAD_REQUEST_MESSAGE);
	}

	private Map<Long, Long> findLikeCounts(List<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Long> counts = new HashMap<>();
		itemStatsRepository.findAllById(itemIds)
				.forEach(stats -> counts.put(stats.getId(), stats.getLikeCount()));
		return counts;
	}

	private Set<Long> findLikedItemIds(Long userId, List<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Set.of();
		}
		return itemLikeRepository.findAllByItemIdsAndUserId(itemIds, userId).stream()
				.map(ItemLike::getItem)
				.map(Item::getId)
				.collect(java.util.stream.Collectors.toSet());
	}

	private Map<Long, String> findThumbnails(List<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> thumbnails = new LinkedHashMap<>();
		imageRepository.findAllByItemIdsOrderByItemIdAndId(itemIds).forEach(image ->
				thumbnails.putIfAbsent(image.getItem().getId(), imageUrl(image))
		);
		return thumbnails;
	}

	private static List<Long> itemIds(List<Item> items) {
		return items.stream().map(Item::getId).toList();
	}

	private List<ItemDetailResponse.ImageInfo> toImageInfos(List<Image> images) {
		List<ItemDetailResponse.ImageInfo> imageInfos = new ArrayList<>(images.size());
		for (int index = 0; index < images.size(); index++) {
			Image image = images.get(index);
			imageInfos.add(new ItemDetailResponse.ImageInfo(
					image.getId(),
					imageUrl(image),
					index + 1
			));
		}
		return imageInfos;
	}

	private String imageUrl(Image image) {
		return image.getObjectKey() == null
				? image.getImageUrl()
				: s3ImageObjectService.presignedReadUrl(image.getObjectKey());
	}

	private static ItemSummary toSummary(
			ItemSummaryProjection item,
			Map<Long, Long> likeCounts,
			Set<Long> likedItemIds,
			Map<Long, String> thumbnails
	) {
		return new ItemSummary(
				item.itemId(),
				item.title(),
				contentPreview(item.content()),
				item.quantity(),
				new ItemSummary.Owner(item.ownerId(), item.ownerNickname()),
				item.itemState(),
				thumbnails.get(item.itemId()),
				likeCounts.getOrDefault(item.itemId(), 0L),
				0L,
				likedItemIds.contains(item.itemId()),
				item.createdAt()
		);
	}

	private static String contentPreview(String content) {
		return content.length() <= CONTENT_PREVIEW_LENGTH
				? content
				: content.substring(0, CONTENT_PREVIEW_LENGTH - 3) + "...";
	}
}

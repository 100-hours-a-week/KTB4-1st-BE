package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupItemRepository;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.image.repository.ImageRepository;
import com.example.KTB_Agile_backend.item.dto.request.CreateItemRequest;
import com.example.KTB_Agile_backend.item.dto.response.ItemCreateResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemDetailResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemPageResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemLike;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.entity.ItemView;
import com.example.KTB_Agile_backend.item.repository.ItemLikeRepository;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import com.example.KTB_Agile_backend.item.repository.ItemViewRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemService {

	private static final int PAGE_SIZE = 20;
	private static final int FETCH_SIZE = PAGE_SIZE + 1;
	private static final int CONTENT_PREVIEW_LENGTH = 70;
	private static final Duration VIEW_COUNT_COOLDOWN = Duration.ofHours(24);
	private static final ZoneOffset API_OFFSET = ZoneOffset.ofHours(9);

	private final ItemRepository itemRepository;
	private final ItemStatsRepository itemStatsRepository;
	private final ItemViewRepository itemViewRepository;
	private final ItemLikeRepository itemLikeRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final GroupItemRepository groupItemRepository;
	private final ImageRepository imageRepository;
	private final UserRepository userRepository;

	@Transactional
	public ItemCreateResponse create(Long userId, CreateItemRequest request) {
		User user = userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		List<Group> groups = findRegistrableGroups(userId, request.groupIds());
		List<Image> images = findAttachableImages(userId, request.imageIds());

		Item item = itemRepository.saveAndFlush(new Item(
				user,
				request.title(),
				request.content(),
				request.quantity(),
				request.itemState(),
				request.exchangeUrgencyScore(),
				request.valueGapToleranceScore()
		));
		itemStatsRepository.save(new ItemStats(item));
		groupItemRepository.saveAll(groups.stream()
				.map(group -> new GroupItem(group, item))
				.toList());
		images.forEach(image -> image.attachTo(item));
		imageRepository.saveAll(images);

		return new ItemCreateResponse(item.getId());
	}

	@Transactional
	public ItemDetailResponse findDetail(Long userId, Long itemId) {
		Item item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "물품을 찾을 수 없습니다."));
		ItemStats stats = itemStatsRepository.findByIdForUpdate(itemId).orElse(null);
		if (stats != null) {
			User viewer = userRepository.findActiveById(userId)
					.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
			countViewIfNeeded(item, viewer, stats);
		}

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
				// ponytail: exchange request domain is not implemented yet; replace with its aggregate count later.
				0L,
				itemLikeRepository.existsByItem_IdAndUser_Id(itemId, userId),
				toOffsetDateTime(item.getCreatedAt()),
				toOffsetDateTime(item.getUpdatedAt())
		);
	}

	private void countViewIfNeeded(Item item, User viewer, ItemStats stats) {
		LocalDateTime now = LocalDateTime.now();
		ItemView itemView = itemViewRepository.findByItem_IdAndUser_Id(item.getId(), viewer.getId())
				.orElse(null);
		if (itemView == null) {
			itemViewRepository.save(new ItemView(item, viewer, now));
			stats.increaseViewCount();
			return;
		}

		LocalDateTime nextCountableAt = itemView.getLastCountedAt().plus(VIEW_COUNT_COOLDOWN);
		if (!nextCountableAt.isAfter(now)) {
			itemView.countAt(now);
			stats.increaseViewCount();
		}
	}

	@Transactional(readOnly = true)
	public ItemPageResponse findByGroup(Long userId, Long groupId, String cursor) {
		groupRepository.findByIdAndDeletedAtIsNull(groupId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "그룹을 찾을 수 없습니다."));
		GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "그룹 멤버만 물품을 조회할 수 있습니다."));
		if (member.getStatus() != GroupMemberStatus.ACTIVE) {
			throw new ApiException(ErrorCode.FORBIDDEN, "그룹 멤버만 물품을 조회할 수 있습니다.");
		}

		Long cursorId = decodeCursor(cursor);
		Pageable pageable = PageRequest.of(0, FETCH_SIZE);
		List<Item> items = cursorId == null
				? groupItemRepository.findActiveItemsByGroupId(groupId, pageable)
				: groupItemRepository.findActiveItemsByGroupIdAfter(groupId, cursorId, pageable);
		boolean hasNext = items.size() > PAGE_SIZE;
		List<Item> pageItems = hasNext ? items.subList(0, PAGE_SIZE) : items;
		Map<Long, Long> likeCounts = findLikeCounts(pageItems);
		Set<Long> likedItemIds = findLikedItemIds(userId, pageItems);
		Map<Long, String> thumbnails = findThumbnails(pageItems);
		String nextCursor = hasNext
				? encodeCursor(pageItems.get(pageItems.size() - 1).getId())
				: null;

		return new ItemPageResponse(
				pageItems.stream()
						.map(item -> toSummary(item, likeCounts, likedItemIds, thumbnails))
						.toList(),
				nextCursor,
				hasNext
		);
	}

	private List<Group> findRegistrableGroups(Long userId, Collection<Long> groupIds) {
		List<Group> groups = groupRepository.findAllByIdInAndDeletedAtIsNull(groupIds);
		if (groups.size() != groupIds.size()) {
			throw new ApiException(ErrorCode.NOT_FOUND, "등록할 그룹을 찾을 수 없습니다.");
		}
		long activeMemberships = groupMemberRepository.countByGroup_IdInAndUser_IdAndStatus(
				groupIds,
				userId,
				GroupMemberStatus.ACTIVE
		);
		if (activeMemberships != groupIds.size()) {
			throw new ApiException(ErrorCode.FORBIDDEN, "모든 그룹의 ACTIVE 멤버만 물품을 등록할 수 있습니다.");
		}
		return groups;
	}

	private List<Image> findAttachableImages(Long userId, Collection<Long> imageIds) {
		List<Image> images = imageRepository.findAllForUpdateByIdIn(imageIds);
		if (images.size() != imageIds.size()) {
			throw new ApiException(ErrorCode.NOT_FOUND, "등록할 이미지를 찾을 수 없습니다.");
		}
		for (Image image : images) {
			if (!image.getOwner().getId().equals(userId)) {
				throw new ApiException(ErrorCode.FORBIDDEN, "소유하지 않은 이미지는 등록할 수 없습니다.");
			}
			if (image.getItem() != null) {
				throw new ApiException(ErrorCode.CONFLICT, "이미 다른 물품에 연결된 이미지입니다.");
			}
		}
		return images;
	}

	private Map<Long, Long> findLikeCounts(List<Item> items) {
		if (items.isEmpty()) {
			return Map.of();
		}
		List<Long> itemIds = itemIds(items);
		Map<Long, Long> counts = new HashMap<>();
		itemStatsRepository.findAllById(itemIds)
				.forEach(stats -> counts.put(stats.getId(), stats.getLikeCount()));
		return counts;
	}

	private Set<Long> findLikedItemIds(Long userId, List<Item> items) {
		if (items.isEmpty()) {
			return Set.of();
		}
		return itemLikeRepository.findAllByItemIdsAndUserId(itemIds(items), userId).stream()
				.map(ItemLike::getItem)
				.map(Item::getId)
				.collect(java.util.stream.Collectors.toSet());
	}

	private Map<Long, String> findThumbnails(List<Item> items) {
		if (items.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> thumbnails = new LinkedHashMap<>();
		imageRepository.findAllByItemIdsOrderByItemIdAndId(itemIds(items)).forEach(image ->
				thumbnails.putIfAbsent(image.getItem().getId(), image.getImageUrl())
		);
		return thumbnails;
	}

	private static List<Long> itemIds(List<Item> items) {
		return items.stream().map(Item::getId).toList();
	}

	private static List<ItemDetailResponse.ImageInfo> toImageInfos(List<Image> images) {
		List<ItemDetailResponse.ImageInfo> imageInfos = new ArrayList<>(images.size());
		for (int index = 0; index < images.size(); index++) {
			Image image = images.get(index);
			// ponytail: displayOrder is derived from image ID order; persist it when user-defined ordering is required.
			imageInfos.add(new ItemDetailResponse.ImageInfo(
					image.getId(),
					image.getImageUrl(),
					index + 1
			));
		}
		return imageInfos;
	}

	private static ItemSummary toSummary(
			Item item,
			Map<Long, Long> likeCounts,
			Set<Long> likedItemIds,
			Map<Long, String> thumbnails
	) {
		return new ItemSummary(
				item.getId(),
				item.getTitle(),
				contentPreview(item.getContent()),
				item.getQuantity(),
				new ItemSummary.Owner(item.getUser().getId(), item.getUser().getNickname()),
				item.getItemState(),
				thumbnails.get(item.getId()),
				likeCounts.getOrDefault(item.getId(), 0L),
				// ponytail: exchange request domain is not implemented yet; replace with its aggregate count later.
				0L,
				likedItemIds.contains(item.getId()),
				toOffsetDateTime(item.getCreatedAt())
		);
	}

	private static String contentPreview(String content) {
		return content.length() <= CONTENT_PREVIEW_LENGTH
				? content
				: content.substring(0, CONTENT_PREVIEW_LENGTH - 3) + "...";
	}

	private static OffsetDateTime toOffsetDateTime(LocalDateTime timestamp) {
		return timestamp == null ? null : timestamp.atOffset(API_OFFSET);
	}

	private static Long decodeCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			long id = Long.parseLong(new String(
					Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
			if (id <= 0) {
				throw new IllegalArgumentException();
			}
			return id;
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.BAD_REQUEST, "cursor가 올바르지 않습니다.", List.of(), exception);
		}
	}

	private static String encodeCursor(Long itemId) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
				String.valueOf(itemId).getBytes(StandardCharsets.UTF_8));
	}
}

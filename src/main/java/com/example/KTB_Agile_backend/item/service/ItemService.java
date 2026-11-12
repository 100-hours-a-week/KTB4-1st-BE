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
import com.example.KTB_Agile_backend.image.service.S3ImageObjectService;
import com.example.KTB_Agile_backend.item.dto.request.CreateItemRequest;
import com.example.KTB_Agile_backend.item.dto.request.UpdateItemRequest;
import com.example.KTB_Agile_backend.item.cache.RedisItemDetailCache;
import com.example.KTB_Agile_backend.item.dto.response.ItemCreateResponse;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.entity.ItemView;
import com.example.KTB_Agile_backend.item.entity.ItemPriceStatus;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.exception.ItemErrorCode;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import com.example.KTB_Agile_backend.item.repository.ItemViewRepository;
import com.example.KTB_Agile_backend.ai.text.service.ModerationCheckService;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import com.example.KTB_Agile_backend.item.price.ItemPriceRangeCalculator;
import com.example.KTB_Agile_backend.item.price.application.ItemPriceLookupService;
import com.example.KTB_Agile_backend.item.price.application.ItemPriceEstimationRequestPublisher;
import com.example.KTB_Agile_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

	private static final Duration VIEW_COUNT_COOLDOWN = Duration.ofHours(24);

	private final ItemRepository itemRepository;
	private final ItemStatsRepository itemStatsRepository;
	private final ItemViewRepository itemViewRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final GroupItemRepository groupItemRepository;
	private final ImageRepository imageRepository;
	private final S3ImageObjectService s3ImageObjectService;
	private final UserRepository userRepository;
	private final ModerationCheckService moderationCheckService;
	private final ItemPriceLookupService itemPriceLookupService;
	private final ItemPriceRangeCalculator itemPriceRangeCalculator;
	private final ItemPriceEstimationRequestPublisher priceEstimationRequestPublisher;
	private final NotificationService notificationService;
	private final RedisItemDetailCache itemDetailCache;

	@Transactional
	public ItemCreateResponse create(Long userId, CreateItemRequest request) {
		User user = userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		List<Group> groups = findRegistrableGroups(userId, request.groupIds());
		s3ImageObjectService.validatePendingObjects(userId, request.objectKeys());
		if (request.thumbnailObjectKey() != null) {
			s3ImageObjectService.validatePendingObject(userId, request.thumbnailObjectKey());
		}
		Map<String, String> detailThumbnailKeysByObjectKey = validateDetailThumbnailKeys(
				userId, request.objectKeys(), request.thumbnailObjectKey(), request.imageDetailThumbnails());
		if (imageRepository.existsByObjectKeyIn(request.objectKeys())) {
			throw new ApiException(ItemErrorCode.ITEM_IMAGE_ALREADY_REGISTERED);
		}
		List<Image> images = request.objectKeys().stream()
				.map(objectKey -> {
					Image image = Image.fromS3Object(user, objectKey);
					image.setDetailThumbnailObjectKey(detailThumbnailKeysByObjectKey.get(objectKey));
					return image;
				})
				.toList();
		Image thumbnailImage = images.get(0);
		thumbnailImage.setThumbnailObjectKey(request.thumbnailObjectKey());

		String keyword = moderationCheckService.consumeForItem(
				userId,
				request.moderationCheckId(),
				request.title(),
				request.content()
		);
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
		var cachedPrice = itemPriceLookupService.cachedPrice(
				normalizedKeyword,
				request.valueGapToleranceScore(),
				request.exchangeUrgencyScore()
		);
		Item item = new Item(
				user,
				request.title(),
				request.content(),
				request.quantity(),
				request.itemState(),
				request.exchangeUrgencyScore(),
				request.valueGapToleranceScore()
		);
		if (cachedPrice.isPresent()) {
			var priceRange = cachedPrice.get();
			item.setPriceResult(priceRange.unitPrice(), priceRange.minUnitPrice(), priceRange.maxUnitPrice());
		} else {
			String requestId = UUID.randomUUID().toString();
			item.requestPrice(requestId);
		}
		itemRepository.save(item);
		if (cachedPrice.isEmpty()) {
			priceEstimationRequestPublisher.request(item, normalizedKeyword);
		}
		itemStatsRepository.save(new ItemStats(item));
		groupItemRepository.saveAll(groups.stream()
				.map(group -> new GroupItem(group, item))
				.toList());
		images.forEach(image -> image.attachTo(item));
		imageRepository.saveAll(images);
		item.setThumbnailImage(thumbnailImage);
		List<String> objectKeysToRegister = new ArrayList<>(request.objectKeys());
		if (request.thumbnailObjectKey() != null && !objectKeysToRegister.contains(request.thumbnailObjectKey())) {
			objectKeysToRegister.add(request.thumbnailObjectKey());
		}
		objectKeysToRegister.addAll(detailThumbnailKeysByObjectKey.values());
		s3ImageObjectService.markRegistered(objectKeysToRegister);

		return new ItemCreateResponse(item.getId());
	}

	private Map<String, String> validateDetailThumbnailKeys(
			Long userId,
			List<String> objectKeys,
			String thumbnailObjectKey,
			List<CreateItemRequest.ImageDetailThumbnail> mappings
	) {
		if (mappings == null || mappings.isEmpty()) {
			return Map.of();
		}

		Set<String> objectKeySet = Set.copyOf(objectKeys);
		Map<String, String> detailThumbnailKeysByObjectKey = new LinkedHashMap<>();
		Set<String> detailThumbnailKeys = new HashSet<>();
		for (CreateItemRequest.ImageDetailThumbnail mapping : mappings) {
			String objectKey = mapping.objectKey();
			String detailThumbnailObjectKey = mapping.detailThumbnailObjectKey();
			if (!objectKeySet.contains(objectKey)
					|| detailThumbnailKeysByObjectKey.containsKey(objectKey)
					|| !detailThumbnailKeys.add(detailThumbnailObjectKey)
					|| objectKeySet.contains(detailThumbnailObjectKey)
					|| detailThumbnailObjectKey.equals(thumbnailObjectKey)) {
				throw new ApiException(ErrorCode.BAD_REQUEST, "상세용 썸네일 연결 정보가 올바르지 않습니다.");
			}
			detailThumbnailKeysByObjectKey.put(objectKey, detailThumbnailObjectKey);
		}
		if (!detailThumbnailKeysByObjectKey.keySet().equals(objectKeySet)) {
			throw new ApiException(ErrorCode.BAD_REQUEST, "상세용 썸네일 연결 정보가 올바르지 않습니다.");
		}
		detailThumbnailKeys.forEach(key -> s3ImageObjectService.validatePendingObject(userId, key));
		return detailThumbnailKeysByObjectKey;
	}

	@Transactional
	public void update(Long userId, Long itemId, UpdateItemRequest request) {
		userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		Item item = itemRepository.findAllForExchange(List.of(itemId)).stream()
				.filter(candidate -> !candidate.isDeleted())
				.findFirst()
				.orElseThrow(() -> new ApiException(ItemErrorCode.ITEM_NOT_FOUND));
		if (!item.getUser().getId().equals(userId)) {
			throw new ApiException(ItemErrorCode.ITEM_UPDATE_FORBIDDEN);
		}
		int imageCount = request.imageIds().size() + request.objectKeys().size();
		if (imageCount < 1 || imageCount > 3) {
			throw new ApiException(ErrorCode.BAD_REQUEST, "이미지는 1장 이상 3장 이하로 등록해야 합니다.");
		}
		ItemState previousItemState = item.getItemState();

		List<Group> groups = findRegistrableGroups(userId, request.groupIds());
		List<Image> images = new ArrayList<>(findUpdatableImages(userId, itemId, request.imageIds()));
		s3ImageObjectService.validatePendingObjects(userId, request.objectKeys());
		Map<String, String> detailThumbnailKeysByObjectKey = validateDetailThumbnailKeys(
				userId, request.objectKeys(), request.thumbnailObjectKey(), request.imageDetailThumbnails());
		if (!request.objectKeys().isEmpty() && imageRepository.existsByObjectKeyIn(request.objectKeys())) {
			throw new ApiException(ItemErrorCode.ITEM_IMAGE_ALREADY_REGISTERED);
		}
		List<Image> newImages = request.objectKeys().stream()
				.map(objectKey -> {
					Image image = Image.fromS3Object(item.getUser(), objectKey);
					image.setDetailThumbnailObjectKey(detailThumbnailKeysByObjectKey.get(objectKey));
					return image;
				})
				.toList();
		images.addAll(newImages);
		Image thumbnailImage = request.imageIds().isEmpty()
				? newImages.get(0)
				: images.stream()
						.filter(image -> request.imageIds().get(0).equals(image.getId()))
						.findFirst()
						.orElseThrow();
		if (request.thumbnailObjectKey() != null) {
			s3ImageObjectService.validatePendingObject(userId, request.thumbnailObjectKey());
			thumbnailImage.setThumbnailObjectKey(request.thumbnailObjectKey());
		}
		item.update(
				request.title(),
				request.content(),
				request.quantity(),
				request.itemState(),
				request.exchangeUrgencyScore(),
				request.valueGapToleranceScore()
		);
		if (previousItemState == ItemState.AVAILABLE && item.getItemState() == ItemState.UNAVAILABLE) {
			notificationService.itemUnavailable(item, null);
		}
		if (item.getPriceStatus() == ItemPriceStatus.SUCCESS) {
			ItemPriceRangeCalculator.PriceRange priceRange = itemPriceRangeCalculator.calculate(
					item.getUnitPrice(), request.valueGapToleranceScore(), request.exchangeUrgencyScore());
			item.setUnitPrices(item.getUnitPrice(), priceRange.minUnitPrice(), priceRange.maxUnitPrice());
		}
		replaceGroups(item, groups);
		replaceImages(item, images);
		if (!newImages.isEmpty()) {
			imageRepository.saveAll(newImages);
		}
		item.setThumbnailImage(thumbnailImage);
		List<String> objectKeysToRegister = new ArrayList<>(request.objectKeys());
		if (request.thumbnailObjectKey() != null && !objectKeysToRegister.contains(request.thumbnailObjectKey())) {
			objectKeysToRegister.add(request.thumbnailObjectKey());
		}
		objectKeysToRegister.addAll(detailThumbnailKeysByObjectKey.values());
		if (!objectKeysToRegister.isEmpty()) {
			s3ImageObjectService.markRegistered(objectKeysToRegister);
		}
		itemDetailCache.evictAfterCommit(List.of(itemId));
	}

	@Transactional
	public void delete(Long userId, Long itemId) {
		userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		Item item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(ItemErrorCode.ITEM_NOT_FOUND));
		if (!item.getUser().getId().equals(userId)) {
			throw new ApiException(ItemErrorCode.ITEM_DELETE_FORBIDDEN);
		}
		item.delete();
		itemDetailCache.evictAfterCommit(List.of(itemId));
	}

	@Transactional
	public void recordView(Long userId, Long itemId) {
		Item item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(ItemErrorCode.ITEM_NOT_FOUND));
		User viewer = userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		countViewIfNeeded(item, viewer);
	}

	private void countViewIfNeeded(Item item, User viewer) {
		LocalDateTime now = LocalDateTime.now();
		ItemView itemView = itemViewRepository.findByItem_IdAndUser_Id(item.getId(), viewer.getId())
				.orElse(null);
		if (itemView == null) {
			if (itemStatsRepository.incrementViewCount(item.getId()) == 0) {
				return;
			}
			itemViewRepository.save(new ItemView(item, viewer, now));
			return;
		}

		LocalDateTime nextCountableAt = itemView.getLastCountedAt().plus(VIEW_COUNT_COOLDOWN);
		if (!nextCountableAt.isAfter(now)) {
			if (itemStatsRepository.incrementViewCount(item.getId()) == 0) {
				return;
			}
			itemView.countAt(now);
		}
	}

	private List<Group> findRegistrableGroups(Long userId, Collection<Long> groupIds) {
		List<Group> groups = groupRepository.findAllByIdInAndDeletedAtIsNull(groupIds);
		if (groups.size() != groupIds.size()) {
			throw new ApiException(ItemErrorCode.ITEM_REGISTRATION_GROUP_NOT_FOUND);
		}
		long activeMemberships = groupMemberRepository.countByGroup_IdInAndUser_IdAndStatus(
				groupIds,
				userId,
				GroupMemberStatus.ACTIVE
		);
		if (activeMemberships != groupIds.size()) {
			throw new ApiException(ItemErrorCode.ITEM_REGISTRATION_GROUP_MEMBERSHIP_REQUIRED);
		}
		return groups;
	}

	private List<Image> findUpdatableImages(Long userId, Long itemId, Collection<Long> imageIds) {
		if (imageIds.isEmpty()) {
			return List.of();
		}
		List<Image> images = imageRepository.findAllForUpdateByIdIn(imageIds);
		if (images.size() != imageIds.size()) {
			throw new ApiException(ItemErrorCode.ITEM_UPDATE_IMAGE_NOT_FOUND);
		}
		for (Image image : images) {
			if (!image.getOwner().getId().equals(userId)) {
				throw new ApiException(ItemErrorCode.ITEM_UPDATE_IMAGE_NOT_OWNED);
			}
			if (image.getItem() != null && !image.getItem().getId().equals(itemId)) {
				throw new ApiException(ItemErrorCode.ITEM_UPDATE_IMAGE_CONFLICT);
			}
		}
		return images;
	}

	private void replaceGroups(Item item, List<Group> groups) {
		List<GroupItem> currentGroupItems = groupItemRepository.findAllByItemId(item.getId());
		Map<Long, GroupItem> currentByGroupId = new HashMap<>();
		for (GroupItem groupItem : currentGroupItems) {
			currentByGroupId.put(groupItem.getGroup().getId(), groupItem);
		}

		Set<Long> requestedGroupIds = groups.stream().map(Group::getId).collect(java.util.stream.Collectors.toSet());
		currentGroupItems.stream()
				.filter(groupItem -> !requestedGroupIds.contains(groupItem.getGroup().getId()))
				.forEach(GroupItem::delete);

		List<GroupItem> groupItemsToSave = new ArrayList<>();
		for (Group group : groups) {
			GroupItem groupItem = currentByGroupId.get(group.getId());
			if (groupItem == null) {
				groupItemsToSave.add(new GroupItem(group, item));
			} else {
				groupItem.restore();
			}
		}
		groupItemRepository.saveAll(groupItemsToSave);
	}

	private void replaceImages(Item item, List<Image> images) {
		List<Image> currentImages = imageRepository.findAllByItem_IdOrderByIdAsc(item.getId());
		Set<Long> requestedImageIds = images.stream().map(Image::getId)
				.collect(java.util.stream.Collectors.toSet());
		currentImages.stream()
				.filter(image -> !requestedImageIds.contains(image.getId()))
				.forEach(Image::detach);
		images.stream()
				.filter(image -> image.getItem() == null)
				.forEach(image -> image.attachTo(item));
	}

}

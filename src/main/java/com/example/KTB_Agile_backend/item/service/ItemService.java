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
import com.example.KTB_Agile_backend.item.dto.response.ItemCreateResponse;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.entity.ItemView;
import com.example.KTB_Agile_backend.item.exception.ItemErrorCode;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import com.example.KTB_Agile_backend.item.repository.ItemViewRepository;
import com.example.KTB_Agile_backend.ai.text.service.ModerationCheckService;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private final ItemCashService itemCashService;
	private final ItemPriceRangeCalculator itemPriceRangeCalculator;

	@Transactional
	public ItemCreateResponse create(Long userId, CreateItemRequest request) {
		User user = userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		List<Group> groups = findRegistrableGroups(userId, request.groupIds());
		s3ImageObjectService.validatePendingObjects(userId, request.objectKeys());
		if (imageRepository.existsByObjectKeyIn(request.objectKeys())) {
			throw new ApiException(ItemErrorCode.ITEM_IMAGE_ALREADY_REGISTERED);
		}
		List<Image> images = request.objectKeys().stream()
				.map(objectKey -> Image.fromS3Object(user, objectKey))
				.toList();

		String keyword = moderationCheckService.consumeForItem(
				userId,
				request.moderationCheckId(),
				request.title(),
				request.content()
		);
		ItemPriceRangeCalculator.PriceRange priceRange = itemCashService.resolvePrice(
				request.title(),
				request.content(),
				keyword,
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
		item.setUnitPrices(priceRange.unitPrice(), priceRange.minUnitPrice(), priceRange.maxUnitPrice());
		itemRepository.save(item);
		itemStatsRepository.save(new ItemStats(item));
		groupItemRepository.saveAll(groups.stream()
				.map(group -> new GroupItem(group, item))
				.toList());
		images.forEach(image -> image.attachTo(item));
		imageRepository.saveAll(images);
		s3ImageObjectService.markRegistered(request.objectKeys());

		return new ItemCreateResponse(item.getId());
	}

	@Transactional
	public void update(Long userId, Long itemId, UpdateItemRequest request) {
		userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		Item item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(ItemErrorCode.ITEM_NOT_FOUND));
		if (!item.getUser().getId().equals(userId)) {
			throw new ApiException(ItemErrorCode.ITEM_UPDATE_FORBIDDEN);
		}

		List<Group> groups = findRegistrableGroups(userId, request.groupIds());
		List<Image> images = findUpdatableImages(userId, itemId, request.imageIds());
		item.update(
				request.title(),
				request.content(),
				request.quantity(),
				request.itemState(),
				request.exchangeUrgencyScore(),
				request.valueGapToleranceScore()
		);
		ItemPriceRangeCalculator.PriceRange priceRange = itemPriceRangeCalculator.calculate(
				item.getUnitPrice(),
				request.valueGapToleranceScore(),
				request.exchangeUrgencyScore()
		);
		item.setUnitPrices(item.getUnitPrice(), priceRange.minUnitPrice(), priceRange.maxUnitPrice());
		replaceGroups(item, groups);
		replaceImages(item, images);
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
	}

	@Transactional
	public void recordView(Long userId, Long itemId) {
		Item item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(ItemErrorCode.ITEM_NOT_FOUND));
		ItemStats stats = itemStatsRepository.findByIdForUpdate(itemId).orElse(null);
		if (stats != null) {
			User viewer = userRepository.findActiveById(userId)
					.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
			countViewIfNeeded(item, viewer, stats);
		}
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

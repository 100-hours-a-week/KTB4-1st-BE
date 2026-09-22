package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.repository.GroupItemRepository;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.image.repository.ImageRepository;
import com.example.KTB_Agile_backend.item.dto.request.CreateItemRequest;
import com.example.KTB_Agile_backend.item.dto.request.UpdateItemRequest;
import com.example.KTB_Agile_backend.item.dto.response.ItemDetailResponse;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.entity.ItemView;
import com.example.KTB_Agile_backend.item.repository.ItemLikeRepository;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import com.example.KTB_Agile_backend.item.repository.ItemViewRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

	@Test
	void createsItemWithAllGroupsAndImagesInOneTransaction() {
		ItemRepository itemRepository = mock(ItemRepository.class);
		ItemStatsRepository itemStatsRepository = mock(ItemStatsRepository.class);
		ItemViewRepository itemViewRepository = mock(ItemViewRepository.class);
		ItemLikeRepository itemLikeRepository = mock(ItemLikeRepository.class);
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		GroupItemRepository groupItemRepository = mock(GroupItemRepository.class);
		ImageRepository imageRepository = mock(ImageRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		ItemService service = new ItemService(
				itemRepository,
				itemStatsRepository,
				itemViewRepository,
				itemLikeRepository,
				groupRepository,
				groupMemberRepository,
				groupItemRepository,
				imageRepository,
				userRepository
		);
		User user = mock(User.class);
		when(user.getId()).thenReturn(42L);
		Group firstGroup = group("첫 그룹");
		Group secondGroup = group("두 번째 그룹");
		Image image = new Image(user, "https://example.com/image.jpg");
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(user));
		when(groupRepository.findAllByIdInAndDeletedAtIsNull(List.of(101L, 205L)))
				.thenReturn(List.of(firstGroup, secondGroup));
		when(groupMemberRepository.countByGroup_IdInAndUser_IdAndStatus(
				eq(List.of(101L, 205L)), eq(42L), any()))
				.thenReturn(2L);
		when(imageRepository.findAllForUpdateByIdIn(List.of(1001L))).thenReturn(List.of(image));
		when(itemRepository.saveAndFlush(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(42L, request(List.of(101L, 205L), List.of(1001L)));

		assertThat(response.itemId()).isNull();
		assertThat(image.getItem()).isNotNull();
		verify(itemStatsRepository).save(any());
		ArgumentCaptor<Iterable<com.example.KTB_Agile_backend.group.entity.GroupItem>> groupItemsCaptor =
				ArgumentCaptor.forClass(Iterable.class);
		verify(groupItemRepository).saveAll(groupItemsCaptor.capture());
		assertThat(((Iterable<?>) groupItemsCaptor.getValue())).hasSize(2);
		verify(imageRepository).saveAll(List.of(image));
	}

	@Test
	void updatesOwnedItemAndReplacesValues() {
		ItemRepository itemRepository = mock(ItemRepository.class);
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		GroupItemRepository groupItemRepository = mock(GroupItemRepository.class);
		ImageRepository imageRepository = mock(ImageRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		ItemService service = new ItemService(
				itemRepository,
				mock(ItemStatsRepository.class),
				mock(ItemViewRepository.class),
				mock(ItemLikeRepository.class),
				groupRepository,
				groupMemberRepository,
				groupItemRepository,
				imageRepository,
				userRepository
		);
		User owner = user(42L);
		Item item = spy(new Item(owner, "기존 제목", "기존 내용"));
		doReturn(123L).when(item).getId();
		Group group = mock(Group.class);
		when(group.getId()).thenReturn(101L);
		Image image = new Image(owner, "https://example.com/image.jpg");
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(owner));
		when(itemRepository.findByIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(item));
		when(groupRepository.findAllByIdInAndDeletedAtIsNull(List.of(101L)))
				.thenReturn(List.of(group));
		when(groupMemberRepository.countByGroup_IdInAndUser_IdAndStatus(
				eq(List.of(101L)), eq(42L), any()))
				.thenReturn(1L);
		when(imageRepository.findAllForUpdateByIdIn(List.of(1001L))).thenReturn(List.of(image));
		when(groupItemRepository.findAllByItemId(123L)).thenReturn(List.of());
		when(imageRepository.findAllByItem_IdOrderByIdAsc(123L)).thenReturn(List.of());

		service.update(42L, 123L, new UpdateItemRequest(
				"새 제목",
				"새 내용",
				2,
				ItemState.COMPLETED,
				new BigDecimal("0.70"),
				new BigDecimal("0.80"),
				List.of(101L),
				List.of(1001L)
		));

		assertThat(List.of(
				item.getTitle(),
				item.getContent(),
				item.getQuantity(),
				item.getItemState(),
				image.getItem()
		)).containsExactly("새 제목", "새 내용", 2, ItemState.COMPLETED, item);
		verify(groupItemRepository).saveAll(any());
		verify(imageRepository).saveAll(List.of(image));
	}

	@Test
	void rejectsWholeRequestWhenOneGroupIsNotAvailable() {
		ItemRepository itemRepository = mock(ItemRepository.class);
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		ItemService service = service(itemRepository, groupRepository, groupMemberRepository,
				mock(ImageRepository.class), userRepository);
		User requester = user(42L);
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(requester));
		when(groupRepository.findAllByIdInAndDeletedAtIsNull(List.of(101L, 205L)))
				.thenReturn(List.of(group("첫 그룹")));

		ApiException exception = assertThrows(ApiException.class,
				() -> service.create(42L, request(List.of(101L, 205L), List.of(1001L))));

		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(itemRepository, never()).saveAndFlush(any());
		verify(groupMemberRepository, never()).countByGroup_IdInAndUser_IdAndStatus(any(), any(), any());
	}

	@Test
	void rejectsImageOwnedByAnotherUser() {
		ImageRepository imageRepository = mock(ImageRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		ItemService service = service(mock(ItemRepository.class), groupRepository,
				groupMemberRepository, imageRepository, userRepository);
		User owner = user(7L);
		User requester = user(42L);
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(requester));
		when(groupRepository.findAllByIdInAndDeletedAtIsNull(List.of(101L)))
				.thenReturn(List.of(group("그룹")));
		when(groupMemberRepository.countByGroup_IdInAndUser_IdAndStatus(any(), eq(42L), any()))
				.thenReturn(1L);
		when(imageRepository.findAllForUpdateByIdIn(List.of(1001L)))
				.thenReturn(List.of(new Image(owner, "https://example.com/image.jpg")));

		ApiException exception = assertThrows(ApiException.class,
				() -> service.create(42L, request(List.of(101L), List.of(1001L))));

		assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void rejectsLeftMemberFromGroupItemList() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		ItemService service = service(mock(ItemRepository.class), groupRepository,
				groupMemberRepository, mock(ImageRepository.class));
		Group group = group("그룹");
		GroupMember member = new GroupMember(group, mock(User.class));
		member.leave(LocalDateTime.now());
		when(groupRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(group));
		when(groupMemberRepository.findByGroup_IdAndUser_Id(101L, 42L)).thenReturn(Optional.of(member));

		ApiException exception = assertThrows(ApiException.class,
				() -> service.findByGroup(42L, 101L, null));

		assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void returnsCursorPageAndNullCursorOnLastPage() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		GroupItemRepository groupItemRepository = mock(GroupItemRepository.class);
		ItemStatsRepository itemStatsRepository = mock(ItemStatsRepository.class);
		ItemViewRepository itemViewRepository = mock(ItemViewRepository.class);
		ItemLikeRepository itemLikeRepository = mock(ItemLikeRepository.class);
		ImageRepository imageRepository = mock(ImageRepository.class);
		User owner = user(10L);
		User memberUser = user(42L);
		ItemService service = new ItemService(
				mock(ItemRepository.class), itemStatsRepository, itemViewRepository, itemLikeRepository, groupRepository,
				groupMemberRepository, groupItemRepository, imageRepository, mock(UserRepository.class));
		Group group = group("그룹");
		when(groupRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(group));
		when(groupMemberRepository.findByGroup_IdAndUser_Id(101L, 42L))
				.thenReturn(Optional.of(new GroupMember(group, memberUser)));
		List<Item> firstItems = new ArrayList<>();
		for (long id = 21; id >= 1; id--) {
			firstItems.add(item(id, owner));
		}
		Item lastItem = item(1L, owner);
		when(groupItemRepository.findActiveItemsByGroupId(eq(101L), any(Pageable.class)))
				.thenReturn(firstItems);
		when(groupItemRepository.findActiveItemsByGroupIdAfter(eq(101L), eq(2L), any(Pageable.class)))
				.thenReturn(List.of(lastItem));
		when(itemStatsRepository.findAllById(any())).thenReturn(List.of());
		when(itemLikeRepository.findAllByItemIdsAndUserId(any(), eq(42L))).thenReturn(List.of());
		when(imageRepository.findAllByItemIdsOrderByItemIdAndId(any())).thenReturn(List.of());

		var firstResponse = service.findByGroup(42L, 101L, null);
		var lastResponse = service.findByGroup(42L, 101L, "Mg");

		assertThat(firstResponse.items()).hasSize(20);
		assertThat(firstResponse.items().get(0).itemId()).isEqualTo(21L);
		assertThat(firstResponse.nextCursor()).isEqualTo("Mg");
		assertThat(firstResponse.hasNext()).isTrue();
		assertThat(lastResponse.items()).hasSize(1);
		assertThat(lastResponse.nextCursor()).isNull();
		assertThat(lastResponse.hasNext()).isFalse();
	}

	@Test
	void returnsItemDetailWithRelatedDataAndCurrentUserLikeStatus() {
		ItemRepository itemRepository = mock(ItemRepository.class);
		ItemStatsRepository itemStatsRepository = mock(ItemStatsRepository.class);
		ItemViewRepository itemViewRepository = mock(ItemViewRepository.class);
		ItemLikeRepository itemLikeRepository = mock(ItemLikeRepository.class);
		GroupItemRepository groupItemRepository = mock(GroupItemRepository.class);
		ImageRepository imageRepository = mock(ImageRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		ItemService service = new ItemService(
				itemRepository,
				itemStatsRepository,
				itemViewRepository,
				itemLikeRepository,
				mock(GroupRepository.class),
				mock(GroupMemberRepository.class),
				groupItemRepository,
				imageRepository,
				userRepository
		);

		User owner = mock(User.class);
		when(owner.getId()).thenReturn(10L);
		when(owner.getNickname()).thenReturn("사용자1");
		when(owner.getProfileImageUrl()).thenReturn("https://example.com/profile.jpg");
		User viewer = mock(User.class);
		when(viewer.getId()).thenReturn(42L);
		Item item = mock(Item.class);
		when(item.getId()).thenReturn(123L);
		when(item.getUser()).thenReturn(owner);
		when(item.getTitle()).thenReturn("게시글 제목1");
		when(item.getContent()).thenReturn("게시글 내용입니다.");
		when(item.getQuantity()).thenReturn(1);
		when(item.getItemState()).thenReturn(ItemState.AVAILABLE);
		when(item.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 9, 4, 13, 30));
		when(item.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 9, 4, 13, 30));

		Group group = mock(Group.class);
		when(group.getId()).thenReturn(101L);
		when(group.getGroupName()).thenReturn("카테뷰");
		GroupItem groupItem = mock(GroupItem.class);
		when(groupItem.getGroup()).thenReturn(group);

		Image firstImage = mock(Image.class);
		when(firstImage.getId()).thenReturn(501L);
		when(firstImage.getImageUrl()).thenReturn("https://example.com/item1.jpg");
		Image secondImage = mock(Image.class);
		when(secondImage.getId()).thenReturn(502L);
		when(secondImage.getImageUrl()).thenReturn("https://example.com/item2.jpg");

		ItemStats stats = mock(ItemStats.class);
		when(stats.getLikeCount()).thenReturn(33L);
		when(stats.getViewCount()).thenReturn(128L);
		when(itemRepository.findByIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(item));
		when(itemStatsRepository.findByIdForUpdate(123L)).thenReturn(Optional.of(stats));
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(viewer));
		when(itemViewRepository.findByItem_IdAndUser_Id(123L, 42L)).thenReturn(Optional.empty());
		when(groupItemRepository.findActiveGroupItemsByItemId(123L)).thenReturn(List.of(groupItem));
		when(imageRepository.findAllByItem_IdOrderByIdAsc(123L))
				.thenReturn(List.of(firstImage, secondImage));
		when(itemLikeRepository.existsByItem_IdAndUser_Id(123L, 42L)).thenReturn(false);

		ItemDetailResponse response = service.findDetail(42L, 123L);

		assertThat(response).usingRecursiveComparison().isEqualTo(new ItemDetailResponse(
				123L,
				List.of(new ItemDetailResponse.GroupInfo(101L, "카테뷰")),
				"게시글 제목1",
				"게시글 내용입니다.",
				1,
				ItemState.AVAILABLE,
				new ItemDetailResponse.Owner(10L, "사용자1", "https://example.com/profile.jpg"),
				List.of(
						new ItemDetailResponse.ImageInfo(501L, "https://example.com/item1.jpg", 1),
						new ItemDetailResponse.ImageInfo(502L, "https://example.com/item2.jpg", 2)
				),
				33L,
				128L,
				0L,
				false,
				OffsetDateTime.parse("2026-09-04T13:30:00+09:00"),
				OffsetDateTime.parse("2026-09-04T13:30:00+09:00")
		));
		verify(itemViewRepository).save(any());
		verify(stats).increaseViewCount();
	}

	@Test
	void countsViewOnlyAfterTwentyFourHours() {
		ItemRepository itemRepository = mock(ItemRepository.class);
		ItemStatsRepository itemStatsRepository = mock(ItemStatsRepository.class);
		ItemViewRepository itemViewRepository = mock(ItemViewRepository.class);
		ItemLikeRepository itemLikeRepository = mock(ItemLikeRepository.class);
		GroupItemRepository groupItemRepository = mock(GroupItemRepository.class);
		ImageRepository imageRepository = mock(ImageRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		ItemService service = new ItemService(
				itemRepository,
				itemStatsRepository,
				itemViewRepository,
				itemLikeRepository,
				mock(GroupRepository.class),
				mock(GroupMemberRepository.class),
				groupItemRepository,
				imageRepository,
				userRepository
		);

		User viewer = user(42L);
		Item item = item(123L, viewer);
		ItemStats stats = mock(ItemStats.class);
		ItemView recentView = new ItemView(item, viewer, LocalDateTime.now().minusHours(23));
		ItemView expiredView = new ItemView(item, viewer, LocalDateTime.now().minusHours(25));
		when(itemRepository.findByIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(item));
		when(itemStatsRepository.findByIdForUpdate(123L)).thenReturn(Optional.of(stats));
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(viewer));
		when(itemViewRepository.findByItem_IdAndUser_Id(123L, 42L))
				.thenReturn(Optional.of(recentView), Optional.of(expiredView));
		when(groupItemRepository.findActiveGroupItemsByItemId(123L)).thenReturn(List.of());
		when(imageRepository.findAllByItem_IdOrderByIdAsc(123L)).thenReturn(List.of());
		when(itemLikeRepository.existsByItem_IdAndUser_Id(123L, 42L)).thenReturn(false);

		service.findDetail(42L, 123L);
		service.findDetail(42L, 123L);

		verify(stats).increaseViewCount();
	}

	private static CreateItemRequest request(List<Long> groupIds, List<Long> imageIds) {
		return new CreateItemRequest(
				"제목",
				"내용",
				1,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				groupIds,
				imageIds
		);
	}

	private static User user(long id) {
		User user = mock(User.class);
		lenient().when(user.getId()).thenReturn(id);
		return user;
	}

	private static Group group(String name) {
		return Group.create(name, "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
	}

	private static Item item(long id, User owner) {
		Item item = mock(Item.class);
		lenient().when(item.getId()).thenReturn(id);
		lenient().when(item.getUser()).thenReturn(owner);
		lenient().when(item.getTitle()).thenReturn("물품 " + id);
		lenient().when(item.getContent()).thenReturn("내용");
		lenient().when(item.getQuantity()).thenReturn(1);
		lenient().when(item.getItemState()).thenReturn(ItemState.AVAILABLE);
		return item;
	}

	private static ItemService service(
			ItemRepository itemRepository,
			GroupRepository groupRepository,
			GroupMemberRepository groupMemberRepository,
			ImageRepository imageRepository
	) {
		return service(itemRepository, groupRepository, groupMemberRepository, imageRepository,
				mock(UserRepository.class));
	}

	private static ItemService service(
			ItemRepository itemRepository,
			GroupRepository groupRepository,
			GroupMemberRepository groupMemberRepository,
			ImageRepository imageRepository,
			UserRepository userRepository
	) {
		return new ItemService(
				itemRepository,
				mock(ItemStatsRepository.class),
				mock(ItemViewRepository.class),
				mock(ItemLikeRepository.class),
				groupRepository,
				groupMemberRepository,
				mock(GroupItemRepository.class),
				imageRepository,
				userRepository
		);
	}

}

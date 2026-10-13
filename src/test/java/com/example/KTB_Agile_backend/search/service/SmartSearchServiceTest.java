package com.example.KTB_Agile_backend.search.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupItemRepository;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.service.ItemQueryService;
import com.example.KTB_Agile_backend.search.dto.request.SmartSearchRequest;
import com.example.KTB_Agile_backend.search.dto.response.SmartSearchResponse;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartSearchServiceTest {
	private static final Long USER_ID = 42L;
	private static final Long GROUP_ID = 101L;

	@Test
	void skipsAiForBlankPreferenceAndWhenThereAreNoCandidates() {
		Fixture fixture = fixture(GROUP_ID);
		List<ItemSummaryProjection> candidates = List.of(candidate(201L), candidate(202L));
		List<ItemSummary> summaries = List.of(summary(201L), summary(202L));
		when(fixture.groupItemRepository.findSmartSearchCandidates(
				eq(GROUP_ID), eq(USER_ID), any(BigDecimal.class), eq(ItemState.AVAILABLE),
				eq(UserStatus.ACTIVE), eq(GroupMemberStatus.ACTIVE),
				eq(ExchangeRequestStatus.PENDING), any(Pageable.class)))
				.thenReturn(candidates, candidates, List.of());
		when(fixture.itemQueryService.toSummariesForUser(USER_ID, candidates))
				.thenReturn(summaries, summaries);

		SmartSearchResponse blankPreference = fixture.service.search(USER_ID, request(null));
		SmartSearchResponse whitespacePreference = fixture.service.search(USER_ID, request("  "));
		SmartSearchResponse noCandidates = fixture.service.search(USER_ID, request("야식"));

		assertThat(blankPreference.items()).containsExactlyElementsOf(summaries);
		assertThat(whitespacePreference.items()).containsExactlyElementsOf(summaries);
		assertThat(noCandidates.items()).isEmpty();
		verify(fixture.itemQueryService, times(2)).toSummariesForUser(USER_ID, candidates);
	}

	@Test
	void rejectsSelectedItemNotLinkedToRequestedGroup() {
		Fixture fixture = fixture(GROUP_ID + 1);

		assertThatThrownBy(() -> fixture.service.search(USER_ID, request(null)))
				.isInstanceOf(ApiException.class);
		verify(fixture.groupItemRepository).findActiveGroupItemsByItemIds(List.of(501L));
	}

	private static Fixture fixture(Long linkedGroupId) {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		GroupItemRepository groupItemRepository = mock(GroupItemRepository.class);
		ItemRepository itemRepository = mock(ItemRepository.class);
		ItemQueryService itemQueryService = mock(ItemQueryService.class);
		when(groupRepository.existsByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(true);
		GroupMember member = mock(GroupMember.class);
		when(member.getStatus()).thenReturn(GroupMemberStatus.ACTIVE);
		when(groupMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, USER_ID))
				.thenReturn(Optional.of(member));

		User owner = mock(User.class);
		when(owner.getId()).thenReturn(USER_ID);
		Item selectedItem = mock(Item.class);
		when(selectedItem.getId()).thenReturn(501L);
		when(selectedItem.getUser()).thenReturn(owner);
		when(selectedItem.getUnitPrice()).thenReturn(1000L);
		when(selectedItem.getQuantity()).thenReturn(4);
		when(selectedItem.getItemState()).thenReturn(ItemState.AVAILABLE);
		when(itemRepository.findAllByIdInAndDeletedAtIsNull(List.of(501L)))
				.thenReturn(List.of(selectedItem));

		Group group = mock(Group.class);
		when(group.getId()).thenReturn(linkedGroupId);
		GroupItem groupItem = mock(GroupItem.class);
		when(groupItem.getGroup()).thenReturn(group);
		when(groupItem.getItem()).thenReturn(selectedItem);
		when(groupItemRepository.findActiveGroupItemsByItemIds(List.of(501L)))
				.thenReturn(List.of(groupItem));

		SmartSearchService service = new SmartSearchService(
				groupRepository,
				groupMemberRepository,
				groupItemRepository,
				itemRepository,
				itemQueryService,
				new SmartSearchBudgetCalculator(),
				mock(ObjectMapper.class),
				"",
				1,
				1
		);
		return new Fixture(service, groupItemRepository, itemQueryService);
	}

	private static SmartSearchRequest request(String preferenceText) {
		return new SmartSearchRequest(GROUP_ID, List.of(new SmartSearchRequest.SelectedItem(501L, 2)), preferenceText);
	}

	private static ItemSummaryProjection candidate(Long itemId) {
		return new ItemSummaryProjection(itemId, "라면", "", 2, 99L, "판매자", ItemState.AVAILABLE,
				LocalDateTime.now());
	}

	private static ItemSummary summary(Long itemId) {
		return new ItemSummary(itemId, "라면", "", 2, null, ItemState.AVAILABLE, null,
				0L, 0L, 0L, false, LocalDateTime.now());
	}

	private record Fixture(
			SmartSearchService service,
			GroupItemRepository groupItemRepository,
			ItemQueryService itemQueryService
	) {
	}
}

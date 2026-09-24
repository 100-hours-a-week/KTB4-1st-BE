package com.example.KTB_Agile_backend.group.service;

import com.example.KTB_Agile_backend.group.dto.response.GroupSummary;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupQueryServiceTest {

	@Test
	void loadsAtMostFiveMyGroupsWithoutCursor() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupQueryService service = new GroupQueryService(groupRepository);
		when(groupRepository.findMyGroupSummaries(eq(42L), eq(GroupMemberStatus.ACTIVE), any()))
				.thenReturn(List.of(summary(5), summary(4)));

		service.myGroups(42L);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(groupRepository).findMyGroupSummaries(
				eq(42L), eq(GroupMemberStatus.ACTIVE), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
	}

	@Test
	void searchesFirstCursorPageAndReturnsNextCursor() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupQueryService service = new GroupQueryService(groupRepository);
		List<GroupSummary> initialGroups = new ArrayList<>(IntStream.rangeClosed(2, 21)
				.map(id -> 23 - id)
				.mapToObj(GroupQueryServiceTest::summary)
				.toList());
		initialGroups.add(summary(1));
		when(groupRepository.findSearchSummaries(eq(42L), eq(GroupMemberStatus.ACTIVE), any(), any()))
				.thenReturn(initialGroups);

		var response = service.search(42L, " 마을 ", null);

		ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(groupRepository).findSearchSummaries(
				eq(42L), eq(GroupMemberStatus.ACTIVE), keywordCaptor.capture(), pageableCaptor.capture());
		assertThat(keywordCaptor.getValue()).isEqualTo("마을");
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
		assertThat(response.groups()).hasSize(20);
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo("Mg");

		when(groupRepository.findSearchSummariesAfter(
				eq(42L), eq(GroupMemberStatus.ACTIVE), any(), eq(2L), any()))
				.thenReturn(List.of(initialGroups.get(0)));
		var nextResponse = service.search(42L, " 마을 ", "Mg");

		assertThat(nextResponse.groups()).hasSize(1);
		assertThat(nextResponse.hasNext()).isFalse();
		assertThat(nextResponse.nextCursor()).isNull();
		verify(groupRepository).findSearchSummariesAfter(
				eq(42L), eq(GroupMemberStatus.ACTIVE), eq("마을"), eq(2L), any(Pageable.class));
	}

	@Test
	void rejectsInvalidCursor() {
		GroupQueryService service = new GroupQueryService(mock(GroupRepository.class));

		assertThat(org.junit.jupiter.api.Assertions.assertThrows(
				com.example.KTB_Agile_backend.common.exception.ApiException.class,
				() -> service.search(42L, "", "invalid")
		).status()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
	}

	private static GroupSummary summary(int groupId) {
		return new GroupSummary(
				(long) groupId,
				"그룹 " + groupId,
				"주소",
				"설명",
				0L,
				0L,
				null,
				false
		);
	}
}

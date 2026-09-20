package com.example.KTB_Agile_backend.group.service;

import com.example.KTB_Agile_backend.group.entity.Group;
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
	void searchesFirstCursorPageAndReturnsNextCursor() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupQueryService service = new GroupQueryService(groupRepository);
		List<Group> initialGroups = new ArrayList<>(IntStream.rangeClosed(2, 21).map(id -> 23 - id)
				.mapToObj(id -> {
					Group group = mock(Group.class);
					when(group.getId()).thenReturn((long) id);
					return group;
				})
				.toList());
		initialGroups.add(mock(Group.class));
		when(groupRepository.findByDeletedAtIsNullAndGroupNameContainingOrderByIdDesc(any(), any()))
				.thenReturn(initialGroups);

		var response = service.search(" 마을 ", null);

		ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(groupRepository).findByDeletedAtIsNullAndGroupNameContainingOrderByIdDesc(
				keywordCaptor.capture(), pageableCaptor.capture());
		assertThat(keywordCaptor.getValue()).isEqualTo("마을");
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
		assertThat(response.groups()).hasSize(20);
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo("Mg");

		when(groupRepository.findByDeletedAtIsNullAndGroupNameContainingAndIdLessThanOrderByIdDesc(
				any(), eq(2L), any())).thenReturn(List.of(initialGroups.get(0)));
		var nextResponse = service.search(" 마을 ", "Mg");

		assertThat(nextResponse.groups()).hasSize(1);
		assertThat(nextResponse.size()).isEqualTo(10);
		assertThat(nextResponse.hasNext()).isFalse();
		assertThat(nextResponse.nextCursor()).isNull();
		verify(groupRepository).findByDeletedAtIsNullAndGroupNameContainingAndIdLessThanOrderByIdDesc(
				eq("마을"), eq(2L), any(Pageable.class));
	}

	@Test
	void rejectsInvalidCursor() {
		GroupQueryService service = new GroupQueryService(mock(GroupRepository.class));

		assertThat(org.junit.jupiter.api.Assertions.assertThrows(
				com.example.KTB_Agile_backend.common.exception.ApiException.class,
				() -> service.search("", "invalid")
		).status()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
	}
}

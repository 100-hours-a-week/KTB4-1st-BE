package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GroupRepositoryTest {

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findsRecommendationsWithCountsAndJoinStatus() {
		User viewer = new User("viewer");
		User anotherUser = new User("another");
		User thirdUser = new User("third");
		entityManager.persist(viewer);
		entityManager.persist(anotherUser);
		entityManager.persist(thirdUser);

		Group popular = groupRepository.saveAndFlush(createGroup("인기 그룹"));
		Group joined = groupRepository.saveAndFlush(createGroup("가입 그룹"));
		groupMemberRepository.saveAllAndFlush(List.of(
				new GroupMember(popular, anotherUser),
				new GroupMember(popular, thirdUser),
				new GroupMember(joined, viewer)
		));

		entityManager.persist(new GroupItem(popular, 1L));
		entityManager.persist(new GroupItem(popular, 2L));
		entityManager.flush();

		var recommendations = groupRepository.findRecommendations(
				viewer.getId(), GroupMemberStatus.ACTIVE, PageRequest.of(0, 10));

		assertThat(recommendations).hasSize(2);
		assertThat(recommendations.get(0).groupId()).isEqualTo(popular.getId());
		assertThat(recommendations.get(0).memberCount()).isEqualTo(2L);
		assertThat(recommendations.get(0).itemCount()).isEqualTo(2L);
		assertThat(recommendations.get(0).lastItemCreatedAt()).isNotNull();
		assertThat(recommendations.get(0).isJoined()).isFalse();
		assertThat(recommendations.get(1).groupId()).isEqualTo(joined.getId());
		assertThat(recommendations.get(1).isJoined()).isTrue();
	}

	@Test
	void findsSearchResultsWithTheSameSummaryFields() {
		User viewer = new User("viewer");
		entityManager.persist(viewer);
		Group group = groupRepository.saveAndFlush(createGroup("인기 마을"));
		groupMemberRepository.saveAndFlush(new GroupMember(group, viewer));
		entityManager.persist(new GroupItem(group, 1L));
		entityManager.flush();

		var searchResults = groupRepository.findSearchSummaries(
				viewer.getId(),
				GroupMemberStatus.ACTIVE,
				"마을",
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")));

		assertThat(searchResults).hasSize(1);
		assertThat(searchResults.get(0).groupId()).isEqualTo(group.getId());
		assertThat(searchResults.get(0).memberCount()).isEqualTo(1L);
		assertThat(searchResults.get(0).itemCount()).isEqualTo(1L);
		assertThat(searchResults.get(0).isJoined()).isTrue();
	}

	private static Group createGroup(String name) {
		return Group.create(
				name,
				"서울시 중구 세종대로 1",
				new BigDecimal("126.978000"),
				new BigDecimal("37.566500"),
				"그룹 설명"
		);
	}
}

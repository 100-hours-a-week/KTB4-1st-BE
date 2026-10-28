package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GroupItemRepositoryTest {

	@Autowired
	private GroupItemRepository groupItemRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void findsActiveItemSummariesInOneQueryWithIdCursor() {
		User firstSeller = new User("seller-one");
		User secondSeller = new User("seller-two");
		User thirdSeller = new User("seller-three");
		entityManager.persist(firstSeller);
		entityManager.persist(secondSeller);
		entityManager.persist(thirdSeller);
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		entityManager.persist(group);
		entityManager.persist(new GroupMember(group, firstSeller));
		entityManager.persist(new GroupMember(group, secondSeller));
		entityManager.persist(new GroupMember(group, thirdSeller));

		Item first = new Item(firstSeller, "첫 물품", "첫 설명");
		Item second = new Item(secondSeller, "두 번째 물품", "두 번째 설명");
		Item deletedAssociation = new Item(firstSeller, "삭제 연결", "삭제된 연결");
		Item third = new Item(thirdSeller, "세 번째 물품", "세 번째 설명");
		entityManager.persist(first);
		entityManager.persist(second);
		entityManager.persist(deletedAssociation);
		entityManager.persist(third);
		entityManager.flush();

		entityManager.persist(new GroupItem(group, first));
		entityManager.persist(new GroupItem(group, second));
		GroupItem deleted = new GroupItem(group, deletedAssociation);
		deleted.delete();
		entityManager.persist(deleted);
		entityManager.persist(new GroupItem(group, third));
		entityManager.flush();
		entityManager.clear();

		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		List<ItemSummaryProjection> firstBatch = groupItemRepository.findActiveItemSummariesByGroupId(
				group.getId(), UserStatus.ACTIVE, GroupMemberStatus.ACTIVE, PageRequest.of(0, 2));
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
		assertThat(firstBatch).extracting(ItemSummaryProjection::ownerNickname)
				.containsExactly("seller-three", "seller-two");

		statistics.clear();
		List<ItemSummaryProjection> nextBatch = groupItemRepository.findActiveItemSummariesByGroupIdAfter(
				group.getId(), second.getId(), UserStatus.ACTIVE, GroupMemberStatus.ACTIVE, PageRequest.of(0, 2));
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);

		assertThat(firstBatch).extracting(ItemSummaryProjection::itemId)
				.containsExactly(third.getId(), second.getId());
		assertThat(nextBatch).extracting(ItemSummaryProjection::itemId)
				.containsExactly(first.getId());

		entityManager.createQuery("update Item item set item.deletedAt = :deletedAt where item.id = :itemId")
				.setParameter("deletedAt", LocalDateTime.now())
				.setParameter("itemId", first.getId())
				.executeUpdate();
		entityManager.clear();

		statistics.clear();
		assertThat(groupItemRepository.findActiveItemSummariesByGroupId(
				group.getId(), UserStatus.ACTIVE, GroupMemberStatus.ACTIVE, PageRequest.of(0, 10)))
				.extracting(ItemSummaryProjection::itemId)
				.containsExactly(third.getId(), second.getId());
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
		assertThat(groupItemRepository.findActiveItemSummariesByGroupIdAndItemIds(
				group.getId(), List.of(first.getId(), second.getId(), third.getId()),
						UserStatus.ACTIVE, GroupMemberStatus.ACTIVE))
				.extracting(ItemSummaryProjection::itemId)
				.containsExactlyInAnyOrder(second.getId(), third.getId());
	}

	@Test
	void searchFiltersOwnersByActiveUserAndMembershipStatusOnBothPages() {
		User olderActiveSeller = new User("active-older");
		User leftSeller = new User("left-seller");
		User withdrawnSeller = new User("withdrawn-seller");
		withdrawnSeller.withdraw(LocalDateTime.now());
		User newestActiveSeller = new User("active-newest");
		entityManager.persist(olderActiveSeller);
		entityManager.persist(leftSeller);
		entityManager.persist(withdrawnSeller);
		entityManager.persist(newestActiveSeller);

		Group group = Group.create("검색 그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		entityManager.persist(group);
		entityManager.persist(new GroupMember(group, olderActiveSeller));
		GroupMember leftMembership = new GroupMember(group, leftSeller);
		leftMembership.leave(LocalDateTime.now());
		entityManager.persist(leftMembership);
		entityManager.persist(new GroupMember(group, withdrawnSeller));
		entityManager.persist(new GroupMember(group, newestActiveSeller));

		Item olderActiveItem = new Item(olderActiveSeller, "검색 오래된 물품", "설명");
		Item leftItem = new Item(leftSeller, "검색 탈퇴 물품", "설명");
		Item withdrawnItem = new Item(withdrawnSeller, "검색 탈퇴 계정 물품", "설명");
		Item newestActiveItem = new Item(newestActiveSeller, "검색 최신 물품", "설명");
		entityManager.persist(olderActiveItem);
		entityManager.persist(leftItem);
		entityManager.persist(withdrawnItem);
		entityManager.persist(newestActiveItem);
		entityManager.flush();

		entityManager.persist(new GroupItem(group, olderActiveItem));
		entityManager.persist(new GroupItem(group, leftItem));
		entityManager.persist(new GroupItem(group, withdrawnItem));
		entityManager.persist(new GroupItem(group, newestActiveItem));
		entityManager.flush();
		entityManager.clear();

		List<ItemSummaryProjection> firstPage = groupItemRepository.findActiveItemSummariesForSearch(
				group.getId(), "검색", null, UserStatus.ACTIVE, GroupMemberStatus.ACTIVE, PageRequest.of(0, 10));
		List<ItemSummaryProjection> nextPage = groupItemRepository.findActiveItemSummariesForSearchAfter(
				group.getId(), "검색", newestActiveItem.getId(), null,
				UserStatus.ACTIVE, GroupMemberStatus.ACTIVE, PageRequest.of(0, 10));

		assertThat(firstPage).extracting(ItemSummaryProjection::itemId)
				.containsExactly(newestActiveItem.getId(), olderActiveItem.getId());
		assertThat(nextPage).extracting(ItemSummaryProjection::itemId)
				.containsExactly(olderActiveItem.getId());
	}
}

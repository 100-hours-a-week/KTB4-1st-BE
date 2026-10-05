package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupItem;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.user.entity.User;
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
				group.getId(), PageRequest.of(0, 2));
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
		assertThat(firstBatch).extracting(ItemSummaryProjection::ownerNickname)
				.containsExactly("seller-three", "seller-two");

		statistics.clear();
		List<ItemSummaryProjection> nextBatch = groupItemRepository.findActiveItemSummariesByGroupIdAfter(
				group.getId(), second.getId(), PageRequest.of(0, 2));
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
				group.getId(), PageRequest.of(0, 10)))
				.extracting(ItemSummaryProjection::itemId)
				.containsExactly(third.getId(), second.getId());
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
	}
}

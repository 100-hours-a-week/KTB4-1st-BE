package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findsOnlyActiveItemsUsingIdCursor() {
		User user = new User("seller");
		entityManager.persist(user);

		Item first = itemRepository.saveAndFlush(new Item(user, "첫 물품", "첫 설명"));
		Item second = itemRepository.saveAndFlush(new Item(user, "두 번째 물품", "두 번째 설명"));
		Item deleted = itemRepository.saveAndFlush(new Item(user, "삭제 물품", "삭제 설명"));
		Item third = itemRepository.saveAndFlush(new Item(user, "세 번째 물품", "세 번째 설명"));
		entityManager.createQuery("update Item item set item.deletedAt = :deletedAt where item.id = :itemId")
				.setParameter("deletedAt", LocalDateTime.now())
				.setParameter("itemId", deleted.getId())
				.executeUpdate();
		entityManager.clear();

		assertThat(itemRepository.findByIdAndDeletedAtIsNull(deleted.getId())).isEmpty();

		var firstBatch = itemRepository.findAllByDeletedAtIsNullOrderByIdDesc(PageRequest.of(0, 2));
		var nextBatch = itemRepository.findAllByDeletedAtIsNullAndIdLessThanOrderByIdDesc(
				second.getId(), PageRequest.of(0, 2));

		assertThat(firstBatch).extracting(Item::getId)
				.containsExactly(third.getId(), second.getId());
		assertThat(nextBatch).extracting(Item::getId)
				.containsExactly(first.getId());
	}
}

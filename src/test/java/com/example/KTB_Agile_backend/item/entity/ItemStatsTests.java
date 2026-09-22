package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemStatsTests {

	@Autowired
	private EntityManager entityManager;

	@Test
	void usesItemIdAsSharedPrimaryKey() {
		User user = new User("seller");
		entityManager.persist(user);
		Item item = new Item(user, "물품", "설명");
		entityManager.persist(item);
		entityManager.flush();

		ItemStats stats = new ItemStats(item);
		entityManager.persist(stats);
		entityManager.flush();
		entityManager.clear();

		ItemStats persisted = entityManager.find(ItemStats.class, item.getId());

		assertThat(stats.getId()).isEqualTo(item.getId());
		assertThat(persisted.getId()).isEqualTo(item.getId());
		assertThat(persisted.getItem().getId()).isEqualTo(item.getId());
	}
}

package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.CyclicBarrierTestSupport.Pair;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.repository.ItemLikeRepository;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static com.example.KTB_Agile_backend.common.CyclicBarrierTestSupport.concurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@DataJpaTest
@Import(ItemLikeService.class)
class ItemLikeConcurrencyTest {
	@Autowired
	private ItemLikeService service;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private ItemLikeRepository itemLikeRepository;

	@Autowired
	private ItemStatsRepository itemStatsRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@Transactional(propagation = NOT_SUPPORTED)
	void countsConcurrentDuplicateLikesOnce() throws Exception {
		Fixture fixture = transaction(() -> {
			String suffix = UUID.randomUUID().toString();
			User owner = userRepository.save(new User("owner-" + suffix));
			User liker = userRepository.save(new User("liker-" + suffix));
			Item item = itemRepository.save(item(owner));
			itemStatsRepository.save(new ItemStats(item));
			return new Fixture(liker.getId(), item.getId());
		});
		Callable<Boolean> like = () -> {
			try {
				service.like(fixture.userId(), fixture.itemId());
				return true;
			} catch (ApiException exception) {
				if (exception.code().value().equals(ErrorCode.CONFLICT.value())) {
					return false;
				}
				throw exception;
			}
		};

		Pair<Boolean> results = concurrently(like, like);
		assertThat(List.of(results.first(), results.second())).containsExactlyInAnyOrder(true, false);

		transaction(() -> {
			assertThat(itemLikeRepository.existsByItem_IdAndUser_Id(fixture.itemId(), fixture.userId())).isTrue();
			assertThat(itemStatsRepository.findById(fixture.itemId()).orElseThrow().getLikeCount()).isEqualTo(1);
			return null;
		});
	}

	private <T> T transaction(Supplier<T> action) {
		return new TransactionTemplate(transactionManager).execute(status -> action.get());
	}

	private static Item item(User owner) {
		return new Item(owner, "물품", "설명", 5, ItemState.AVAILABLE,
				new BigDecimal("0.50"), new BigDecimal("0.50"));
	}

	private record Fixture(Long userId, Long itemId) {
	}
}

package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.CyclicBarrierTestSupport.Pair;
import com.example.KTB_Agile_backend.ai.text.service.ModerationCheckService;
import com.example.KTB_Agile_backend.group.repository.GroupItemRepository;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.image.service.S3ImageObjectService;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.entity.ItemView;
import com.example.KTB_Agile_backend.item.price.ItemPriceRangeCalculator;
import com.example.KTB_Agile_backend.item.price.application.ItemPriceEstimationRequestPublisher;
import com.example.KTB_Agile_backend.item.price.application.ItemPriceLookupService;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import com.example.KTB_Agile_backend.item.repository.ItemViewRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static com.example.KTB_Agile_backend.common.CyclicBarrierTestSupport.concurrently;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@DataJpaTest
@Import({ItemService.class, ItemPriceRangeCalculator.class})
class ItemViewConcurrencyTest {
	@MockitoBean
	private S3ImageObjectService s3ImageObjectService;

	@MockitoBean
	private ModerationCheckService moderationCheckService;

	@MockitoBean
	private ItemPriceLookupService itemPriceLookupService;

	@MockitoBean
	private ItemPriceEstimationRequestPublisher priceEstimationRequestPublisher;

	@Autowired
	private ItemService service;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private ItemStatsRepository itemStatsRepository;

	@Autowired
	private ItemViewRepository itemViewRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@Transactional(propagation = NOT_SUPPORTED)
	void countsConcurrentViewsAfterCooldownOnce() throws Exception {
		Fixture fixture = new TransactionTemplate(transactionManager).execute(status -> {
			String suffix = UUID.randomUUID().toString();
			User owner = userRepository.save(new User("owner-" + suffix));
			User viewer = userRepository.save(new User("viewer-" + suffix));
			Item item = itemRepository.save(item(owner));
			itemStatsRepository.save(new ItemStats(item));
			itemViewRepository.save(new ItemView(item, viewer, LocalDateTime.now().minusHours(25)));
			return new Fixture(viewer.getId(), item.getId());
		});
		Callable<Boolean> view = () -> {
			service.recordView(fixture.userId(), fixture.itemId());
			return true;
		};

		Pair<Boolean> results = concurrently(view, view);
		assertThat(List.of(results.first(), results.second())).containsOnly(true);

		new TransactionTemplate(transactionManager).execute(status -> {
			assertThat(itemStatsRepository.findById(fixture.itemId()).orElseThrow().getViewCount()).isEqualTo(1);
			ItemView itemView = itemViewRepository.findByItem_IdAndUser_Id(fixture.itemId(), fixture.userId())
					.orElseThrow();
			assertThat(itemView.getLastCountedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
			return null;
		});
	}

	private static Item item(User owner) {
		return new Item(owner, "물품", "설명", 5, ItemState.AVAILABLE,
				new BigDecimal("0.50"), new BigDecimal("0.50"));
	}

	private record Fixture(Long userId, Long itemId) {
	}
}

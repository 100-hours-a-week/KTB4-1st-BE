package com.example.KTB_Agile_backend.item.price.application;

import com.example.KTB_Agile_backend.common.CyclicBarrierTestSupport.Pair;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemPriceStatus;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.price.PriceEstimationFailure;
import com.example.KTB_Agile_backend.item.price.cache.ItemPriceCache;
import com.example.KTB_Agile_backend.item.price.config.PriceEstimationRetryProperties;
import com.example.KTB_Agile_backend.item.price.dto.ItemPriceEstimationResponse;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationOutboxEventRepository;
import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetrySchedule;
import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetryScheduleRepository;
import com.example.KTB_Agile_backend.item.price.retry.PriceEstimationRetryPolicy;
import com.example.KTB_Agile_backend.item.price.retry.PriceEstimationRetryScheduler;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static com.example.KTB_Agile_backend.common.CyclicBarrierTestSupport.concurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import static org.mockito.Mockito.mock;

@DataJpaTest
class ItemPriceConcurrencyTest {
	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PriceEstimationRetryScheduleRepository retryScheduleRepository;

	@Autowired
	private ItemPriceEstimationOutboxEventRepository outboxRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;
	private Fixture cleanupFixture;

	@Test
	@Transactional(propagation = NOT_SUPPORTED)
	void serializesPriceResultAndRetryFailureForSameAttempt() throws Exception {
		String requestId = UUID.randomUUID().toString();
		Fixture fixture = transaction(() -> {
			User owner = userRepository.save(new User("owner-" + UUID.randomUUID()));
			Item item = itemRepository.save(item(owner, requestId));
			return new Fixture(item.getId(), requestId, owner.getId());
		});
		cleanupFixture = fixture;
		ItemPriceEstimationRequested request = new ItemPriceEstimationRequested(
				UUID.randomUUID().toString(), fixture.requestId(), UUID.randomUUID().toString(), fixture.itemId(),
				"book", "title", "content", new BigDecimal("0.30"), new BigDecimal("0.50"), 1);
		ItemPriceResultService resultService = new ItemPriceResultService(
				itemRepository, retryScheduleRepository, mock(ItemPriceCache.class));
		PriceEstimationRetryScheduler retryScheduler = new PriceEstimationRetryScheduler(
				retryScheduleRepository, outboxRepository, itemRepository, retryPolicy(), new ObjectMapper());
		Callable<Boolean> applyResult = () -> transaction(() -> resultService.apply(request,
				new ItemPriceEstimationResponse("book", 100L, 80L, 100L)));
		Callable<Boolean> recordRetry = () -> transaction(() -> {
			retryScheduler.recordFailure(request, PriceEstimationFailure.TRANSIENT);
			return true;
		});

		Pair<Boolean> results = concurrently(applyResult, recordRetry);
		assertThat(results.second()).isTrue();

		State state = transaction(() -> {
			Item item = itemRepository.findById(fixture.itemId()).orElseThrow();
			PriceEstimationRetrySchedule schedule = retryScheduleRepository.findById(fixture.requestId()).orElse(null);
			return new State(item.getPriceStatus(), schedule == null ? null : schedule.getAttemptCount());
		});
		if (results.first()) {
			assertThat(state.priceStatus()).isEqualTo(ItemPriceStatus.SUCCESS);
			assertThat(state.retryAttempt()).isNull();
		} else {
			assertThat(state.priceStatus()).isEqualTo(ItemPriceStatus.PENDING);
			assertThat(state.retryAttempt()).isEqualTo(2);
		}
	}

	@AfterEach
	void cleanUpFixture() {
		if (cleanupFixture == null) {
			return;
		}
		transaction(() -> {
			retryScheduleRepository.deleteById(cleanupFixture.requestId());
			itemRepository.deleteById(cleanupFixture.itemId());
			userRepository.deleteById(cleanupFixture.ownerId());
			return null;
		});
	}

	private <T> T transaction(Supplier<T> action) {
		return new TransactionTemplate(transactionManager).execute(status -> action.get());
	}

	private static Item item(User owner, String requestId) {
		Item item = new Item(owner, "물품", "설명", 5, ItemState.AVAILABLE,
				new BigDecimal("0.50"), new BigDecimal("0.50"));
		item.requestPrice(requestId);
		return item;
	}

	private static PriceEstimationRetryPolicy retryPolicy() {
		return new PriceEstimationRetryPolicy(new PriceEstimationRetryProperties(
				true, 3, true, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), false, Duration.ofMillis(500)));
	}

	private record Fixture(Long itemId, String requestId, Long ownerId) {
	}

	private record State(ItemPriceStatus priceStatus, Integer retryAttempt) {
	}
}

package com.example.KTB_Agile_backend.exchange.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestCreateRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(ExchangeRequestService.class)
class ExchangeRequestConcurrencyTest {

	@Autowired
	private ExchangeRequestService service;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@Transactional(propagation = NOT_SUPPORTED)
	void serializesDuplicatePendingCreatesAndConcurrentCompletion() throws Exception {
		Fixture fixture = new TransactionTemplate(transactionManager).execute(status -> {
			User owner = userRepository.save(new User("owner"));
			User requester = userRepository.save(new User("requester"));
			Item target = itemRepository.save(item(owner, 5));
			Item offered = itemRepository.save(item(requester, 5));
			return new Fixture(owner.getId(), requester.getId(), target.getId(), offered.getId());
		});
		ExchangeRequestCreateRequest request = new ExchangeRequestCreateRequest(2,
				List.of(new ExchangeRequestCreateRequest.OfferedItemRequest(fixture.offeredId(), 3)));

		Callable<Long> create = () -> {
			try {
				return service.create(fixture.requesterId(), fixture.targetId(), request).exchangeRequestId();
			} catch (ApiException exception) {
				if (exception.code().value().equals("CONFLICT")) {
					return -1L;
				}
				throw exception;
			}
		};
		Pair<Long> createResults = concurrently(create, create);
		List<Long> createdIds = List.of(createResults.first(), createResults.second()).stream()
				.filter(id -> id > 0).toList();
		assertThat(createdIds).hasSize(1);

		Callable<Boolean> complete = () -> {
			try {
				service.updateStatus(createdIds.getFirst(), fixture.ownerId(), ExchangeRequestStatus.COMPLETED);
				return true;
			} catch (ApiException exception) {
				if (exception.code().value().equals("CONFLICT")) {
					return false;
				}
				throw exception;
			}
		};
		Pair<Boolean> completionResults = concurrently(complete, complete);
		assertThat(List.of(completionResults.first(), completionResults.second()))
				.containsExactlyInAnyOrder(true, false);

		entityManager.clear();
		assertThat(itemRepository.findByIdAndDeletedAtIsNull(fixture.targetId()).orElseThrow().getQuantity()).isEqualTo(3);
		assertThat(itemRepository.findByIdAndDeletedAtIsNull(fixture.offeredId()).orElseThrow().getQuantity()).isEqualTo(2);
	}

	private <T> Pair<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(2);
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<T> firstResult = executor.submit(() -> {
				barrier.await(10, TimeUnit.SECONDS);
				return first.call();
			});
			Future<T> secondResult = executor.submit(() -> {
				barrier.await(10, TimeUnit.SECONDS);
				return second.call();
			});
			return new Pair<>(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
		}
	}

	private static Item item(User user, int quantity) {
		return new Item(user, "물품", "설명", quantity, ItemState.AVAILABLE,
				new BigDecimal("0.50"), new BigDecimal("0.50"));
	}

	private record Fixture(Long ownerId, Long requesterId, Long targetId, Long offeredId) {
	}

	private record Pair<T>(T first, T second) {
	}
}

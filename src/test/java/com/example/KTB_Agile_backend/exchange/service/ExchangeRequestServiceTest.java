package com.example.KTB_Agile_backend.exchange.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestCreateRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ExchangeRequestService.class)
class ExchangeRequestServiceTest {

	@Autowired
	private ExchangeRequestService service;

	@Autowired
	private ExchangeRequestRepository exchangeRequestRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void createsPendingRequestWithoutReservingStockOrCreatingChatRoom() {
		User owner = persist(new User("owner"));
		User requester = persist(new User("requester"));
		Item target = persist(item(owner, 3, ItemState.AVAILABLE));
		Item offered = persist(item(requester, 4, ItemState.AVAILABLE));

		var response = service.create(requester.getId(), target.getId(), request(2, offered, 3));

		assertThat(response.exchangeRequestId()).isNotNull();
		assertThat(response.requestedStatus()).isEqualTo(ExchangeRequestStatus.PENDING);
		assertThat(response.chatRoomId()).isNull();
		assertThat(response.offeredItems()).extracting("quantity").containsExactly(3);
		assertThat(response.createdAt()).isNotNull();
		assertThat(itemRepository.findByIdAndDeletedAtIsNull(target.getId()).orElseThrow().getQuantity()).isEqualTo(3);
		assertThat(itemRepository.findByIdAndDeletedAtIsNull(offered.getId()).orElseThrow().getQuantity()).isEqualTo(4);
	}

	@Test
	void completesByClampingStockAndKeepingUnavailableStateAndOwnership() {
		User owner = persist(new User("owner"));
		User requester = persist(new User("requester"));
		Item target = persist(item(owner, 2, ItemState.AVAILABLE));
		Item offered = persist(item(requester, 3, ItemState.AVAILABLE));
		var pending = service.create(requester.getId(), target.getId(), request(2, offered, 3));
		target.update(target.getTitle(), target.getContent(), 1, ItemState.AVAILABLE,
				new BigDecimal("0.50"), new BigDecimal("0.50"));
		offered.update(offered.getTitle(), offered.getContent(), 1, ItemState.UNAVAILABLE,
				new BigDecimal("0.50"), new BigDecimal("0.50"));
		entityManager.flush();

		var response = service.updateStatus(pending.exchangeRequestId(), owner.getId(), ExchangeRequestStatus.COMPLETED);

		assertThat(response.status()).isEqualTo(ExchangeRequestStatus.COMPLETED);
		assertThat(response.updatedAt()).isNotNull();
		entityManager.clear();
		Item completedTarget = itemRepository.findByIdAndDeletedAtIsNull(target.getId()).orElseThrow();
		Item completedOffer = itemRepository.findByIdAndDeletedAtIsNull(offered.getId()).orElseThrow();
		assertThat(completedTarget.getQuantity()).isZero();
		assertThat(completedTarget.getItemState()).isEqualTo(ItemState.UNAVAILABLE);
		assertThat(completedOffer.getQuantity()).isZero();
		assertThat(completedOffer.getItemState()).isEqualTo(ItemState.UNAVAILABLE);
		assertThat(completedTarget.getUser().getId()).isEqualTo(owner.getId());
		assertThat(completedOffer.getUser().getId()).isEqualTo(requester.getId());
	}

	@Test
	void rejectedRequestDoesNotDeductStockAndAllowsRetry() {
		User owner = persist(new User("owner"));
		User requester = persist(new User("requester"));
		Item target = persist(item(owner, 2, ItemState.AVAILABLE));
		Item offered = persist(item(requester, 2, ItemState.AVAILABLE));
		var pending = service.create(requester.getId(), target.getId(), request(1, offered, 1));

		assertThatThrownBy(() -> service.updateStatus(pending.exchangeRequestId(), requester.getId(),
				ExchangeRequestStatus.COMPLETED))
				.isInstanceOf(ApiException.class)
				.satisfies(exception -> assertThat(((ApiException) exception).code().value()).isEqualTo("FORBIDDEN"));
		service.updateStatus(pending.exchangeRequestId(), owner.getId(), ExchangeRequestStatus.REJECTED);
		var retry = service.create(requester.getId(), target.getId(), request(1, offered, 1));

		assertThat(retry.requestedStatus()).isEqualTo(ExchangeRequestStatus.PENDING);
		assertThat(retry.exchangeRequestId()).isNotEqualTo(pending.exchangeRequestId());
		assertThat(exchangeRequestRepository.findById(pending.exchangeRequestId()).orElseThrow().getRequestedStatus())
				.isEqualTo(ExchangeRequestStatus.REJECTED);
		assertThat(itemRepository.findByIdAndDeletedAtIsNull(target.getId()).orElseThrow().getQuantity()).isEqualTo(2);
		assertThat(itemRepository.findByIdAndDeletedAtIsNull(offered.getId()).orElseThrow().getQuantity()).isEqualTo(2);
		assertThatThrownBy(() -> service.updateStatus(pending.exchangeRequestId(), owner.getId(),
				ExchangeRequestStatus.COMPLETED))
				.isInstanceOf(ApiException.class)
				.satisfies(exception -> assertThat(((ApiException) exception).code().value()).isEqualTo("CONFLICT"));
	}

	@Test
	void validatesOwnersAvailabilityQuantitiesAndDuplicatePendingRequests() {
		User owner = persist(new User("owner"));
		User requester = persist(new User("requester"));
		Item target = persist(item(owner, 1, ItemState.AVAILABLE));
		Item offered = persist(item(requester, 1, ItemState.AVAILABLE));
		service.create(requester.getId(), target.getId(), request(1, offered, 1));

		assertThatThrownBy(() -> service.create(requester.getId(), target.getId(), request(1, offered, 1)))
				.isInstanceOf(ApiException.class)
				.satisfies(exception -> assertThat(((ApiException) exception).code().value()).isEqualTo("CONFLICT"));
		assertThatThrownBy(() -> service.create(requester.getId(), target.getId(), request(2, offered, 1)))
				.isInstanceOf(ApiException.class)
				.satisfies(exception -> assertThat(((ApiException) exception).code().value())
						.isEqualTo("UNPROCESSABLE_ENTITY"));
		assertThatThrownBy(() -> service.create(owner.getId(), target.getId(), request(1, offered, 1)))
				.isInstanceOf(ApiException.class)
				.satisfies(exception -> assertThat(((ApiException) exception).code().value()).isEqualTo("FORBIDDEN"));
		assertThatThrownBy(() -> service.create(requester.getId(), target.getId(),
				new ExchangeRequestCreateRequest(1, List.of(
						new ExchangeRequestCreateRequest.OfferedItemRequest(offered.getId(), 1),
						new ExchangeRequestCreateRequest.OfferedItemRequest(offered.getId(), 1)))))
				.isInstanceOf(ApiException.class)
				.satisfies(exception -> assertThat(((ApiException) exception).code().value()).isEqualTo("BAD_REQUEST"));
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		entityManager.flush();
		return entity;
	}

	private static Item item(User owner, int quantity, ItemState state) {
		return new Item(owner, "물품", "설명", quantity, state, new BigDecimal("0.50"), new BigDecimal("0.50"));
	}

	private static ExchangeRequestCreateRequest request(int requestedQuantity, Item offered, int offeredQuantity) {
		return new ExchangeRequestCreateRequest(requestedQuantity,
				List.of(new ExchangeRequestCreateRequest.OfferedItemRequest(offered.getId(), offeredQuantity)));
	}
}

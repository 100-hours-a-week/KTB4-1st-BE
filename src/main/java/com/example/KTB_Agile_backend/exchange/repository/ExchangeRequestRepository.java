package com.example.KTB_Agile_backend.exchange.repository;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {

	@Query("""
		select request from ExchangeRequest request
		where request.requester.id = :requesterId
		  and request.item.id = :itemId
		  and request.requestedStatus = :status
		""")
	List<ExchangeRequest> findByRequesterAndItemAndStatus(
			Long requesterId,
			Long itemId,
			ExchangeRequestStatus status
	);

	@Query("""
		select request.id as exchangeRequestId, request.updatedAt as updatedAt
		from ExchangeRequest request
		where request.requestedStatus = :status
		  and (request.requester.id = :userId or request.item.user.id = :userId)
		order by request.updatedAt desc, request.id desc
		""")
	List<CompletedExchangeCursor> findCompletedExchangeCursors(
			@Param("userId") Long userId,
			@Param("status") ExchangeRequestStatus status,
			Pageable pageable
	);

	@Query("""
		select request.id as exchangeRequestId, request.updatedAt as updatedAt
		from ExchangeRequest request
		where request.requestedStatus = :status
		  and (request.requester.id = :userId or request.item.user.id = :userId)
		  and (request.updatedAt < :cursorUpdatedAt
		       or (request.updatedAt = :cursorUpdatedAt and request.id < :cursorId))
		order by request.updatedAt desc, request.id desc
		""")
	List<CompletedExchangeCursor> findCompletedExchangeCursorsAfter(
			@Param("userId") Long userId,
			@Param("status") ExchangeRequestStatus status,
			@Param("cursorUpdatedAt") LocalDateTime cursorUpdatedAt,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);

	@EntityGraph(attributePaths = {"item.user", "offeredItems.item.user"})
	List<ExchangeRequest> findAllByIdIn(Collection<Long> ids);

	@Query("""
		select request.item.id as itemId, count(request.id) as exchangeRequestCount
		from ExchangeRequest request
		where request.item.id in :itemIds
		group by request.item.id
		""")
	List<ItemExchangeRequestCount> findExchangeRequestCountsByItemIds(
			@Param("itemIds") Collection<Long> itemIds
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from ExchangeRequest request where request.id = :requestId")
	Optional<ExchangeRequest> findByIdForUpdate(@Param("requestId") Long requestId);

	interface ItemExchangeRequestCount {
		Long getItemId();

		Long getExchangeRequestCount();
	}

	interface CompletedExchangeCursor {
		Long getExchangeRequestId();

		LocalDateTime getUpdatedAt();
	}
}

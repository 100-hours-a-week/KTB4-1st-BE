package com.example.KTB_Agile_backend.exchange.repository;

import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from ExchangeRequest request where request.id = :requestId")
	Optional<ExchangeRequest> findByIdForUpdate(@Param("requestId") Long requestId);
}

package com.example.KTB_Agile_backend.item.price.persistence;

import com.example.KTB_Agile_backend.item.price.persistence.PriceEstimationRetrySchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceEstimationRetryScheduleRepository extends JpaRepository<PriceEstimationRetrySchedule, String> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select schedule from PriceEstimationRetrySchedule schedule where schedule.requestId = :requestId")
	Optional<PriceEstimationRetrySchedule> findByRequestIdForUpdate(@Param("requestId") String requestId);

	@Query("select schedule.requestId from PriceEstimationRetrySchedule schedule "
			+ "where schedule.nextAttemptAt is not null and schedule.nextAttemptAt <= :now "
			+ "order by schedule.nextAttemptAt")
	List<String> findDueRequestIds(@Param("now") LocalDateTime now, Pageable pageable);
}

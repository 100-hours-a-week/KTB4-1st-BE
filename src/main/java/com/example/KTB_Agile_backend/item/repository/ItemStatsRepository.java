package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.ItemStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemStatsRepository extends JpaRepository<ItemStats, Long> {

	@Modifying
	@Query("update ItemStats stats set stats.viewCount = stats.viewCount + 1 where stats.id = :itemId")
	int incrementViewCount(@Param("itemId") Long itemId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select stats from ItemStats stats where stats.id = :itemId")
	Optional<ItemStats> findByIdForUpdate(@Param("itemId") Long itemId);
}

package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.ItemStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemStatsRepository extends JpaRepository<ItemStats, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select stats from ItemStats stats where stats.id = :itemId")
	Optional<ItemStats> findByIdForUpdate(@Param("itemId") Long itemId);
}
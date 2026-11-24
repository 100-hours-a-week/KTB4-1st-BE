package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.ItemView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ItemViewRepository extends JpaRepository<ItemView, Long> {
	@Query("""
			select itemView.lastCountedAt from ItemView itemView
			where itemView.item.id = :itemId and itemView.user.id = :userId
			""")
	Optional<LocalDateTime> findLastCountedAtWithoutLock(
			@Param("itemId") Long itemId,
			@Param("userId") Long userId
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ItemView> findByItem_IdAndUser_Id(Long itemId, Long userId);
}

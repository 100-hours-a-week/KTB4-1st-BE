package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ItemRepository extends JpaRepository<Item, Long> {

	Optional<Item> findByIdAndDeletedAtIsNull(Long itemId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select item from Item item where item.id in :itemIds order by item.id")
	List<Item> findAllForExchange(@Param("itemIds") Collection<Long> itemIds);

	List<Item> findAllByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

	List<Item> findAllByDeletedAtIsNullAndIdLessThanOrderByIdDesc(
			Long cursorId,
			Pageable pageable
	);
}

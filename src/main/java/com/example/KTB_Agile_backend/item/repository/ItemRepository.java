package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

	Optional<Item> findByIdAndDeletedAtIsNull(Long itemId);

	List<Item> findAllByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

	List<Item> findAllByDeletedAtIsNullAndIdLessThanOrderByIdDesc(
			Long cursorId,
			Pageable pageable
	);
}

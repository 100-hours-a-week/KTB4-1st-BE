package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.ItemView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ItemViewRepository extends JpaRepository<ItemView, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ItemView> findByItem_IdAndUser_Id(Long itemId, Long userId);
}

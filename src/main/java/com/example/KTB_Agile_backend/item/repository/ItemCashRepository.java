package com.example.KTB_Agile_backend.item.repository;

import com.example.KTB_Agile_backend.item.entity.ItemCash;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ItemCashRepository extends JpaRepository<ItemCash, Long> {

	Optional<ItemCash> findByKeywordAndExpiresAtAfter(String keyword, LocalDateTime now);

	Optional<ItemCash> findByKeyword(String keyword);
}

package com.example.KTB_Agile_backend.item.cache;

import com.example.KTB_Agile_backend.item.entity.ItemCash;
import com.example.KTB_Agile_backend.item.repository.ItemCashRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DatabaseItemPriceCache implements ItemPriceCache {

	private final ItemCashRepository itemCashRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<Long> get(String keyword) {
		return itemCashRepository.findByKeywordAndExpiresAtAfter(keyword, LocalDateTime.now())
				.map(ItemCash::getUnitPrice);
	}

	@Override
	@Transactional
	public void put(String keyword, Long unitPrice, Duration ttl) {
		LocalDateTime expiresAt = LocalDateTime.now().plus(ttl);
		ItemCash itemCash = itemCashRepository.findByKeyword(keyword).orElse(null);
		if (itemCash == null) {
			itemCashRepository.save(new ItemCash(keyword, unitPrice, expiresAt));
		} else {
			itemCash.refresh(unitPrice, expiresAt);
		}
	}
}

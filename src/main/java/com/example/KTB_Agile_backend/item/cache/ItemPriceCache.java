package com.example.KTB_Agile_backend.item.cache;

import java.time.Duration;
import java.util.Optional;

public interface ItemPriceCache {
	Optional<Long> get(String keyword);

	void put(String keyword, Long unitPrice, Duration ttl);
}

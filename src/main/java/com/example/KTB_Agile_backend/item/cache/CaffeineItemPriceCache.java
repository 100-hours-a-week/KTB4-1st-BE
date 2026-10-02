package com.example.KTB_Agile_backend.item.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class CaffeineItemPriceCache implements ItemPriceCache {

	private final Cache<String, CachedPrice> cache = Caffeine.newBuilder()
			.expireAfter(new Expiry<String, CachedPrice>() {
				@Override
				public long expireAfterCreate(String key, CachedPrice value, long currentTime) {
					return value.ttlNanos();
				}

				@Override
				public long expireAfterUpdate(
						String key, CachedPrice value, long currentTime, long currentDuration) {
					return value.ttlNanos();
				}

				@Override
				public long expireAfterRead(
						String key, CachedPrice value, long currentTime, long currentDuration) {
					return currentDuration;
				}
			})
			// ponytail: 10,000-entry ceiling; raise it if measured evictions cause repeat AI estimates.
			.maximumSize(10_000)
			.build();

	@Override
	public Optional<Long> get(String keyword) {
		return Optional.ofNullable(cache.getIfPresent(keyword)).map(CachedPrice::unitPrice);
	}

	@Override
	public void put(String keyword, Long unitPrice, Duration ttl) {
		cache.put(keyword, new CachedPrice(unitPrice, ttl.toNanos()));
	}

	private record CachedPrice(Long unitPrice, long ttlNanos) {
	}
}

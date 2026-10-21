package com.example.KTB_Agile_backend.item.price.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisItemPriceCache implements ItemPriceCache {
	private static final String KEY_PREFIX = "item-price:";

	private final StringRedisTemplate redisTemplate;

	@Override
	public Optional<Long> get(String keyword) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + keyword)).map(Long::valueOf);
	}

	@Override
	public void put(String keyword, Long unitPrice, Duration ttl) {
		redisTemplate.opsForValue().set(KEY_PREFIX + keyword, unitPrice.toString(), ttl);
	}
}

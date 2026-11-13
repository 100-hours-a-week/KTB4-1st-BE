package com.example.KTB_Agile_backend.item.cache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisItemDetailCache {
	private static final String KEY_PREFIX = "item:detail:core:";
	private static final Duration TTL = Duration.ofMinutes(5);
	private static final String OPERATION_TIMER = "item.detail.cache.command";
	private static final String RESULT_COUNTER = "item.detail.cache";
	private static final String RESULT_ERROR = "error";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final MeterRegistry meterRegistry;

	public Optional<ItemDetailCacheValue> get(Long itemId) {
		String payload;
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			payload = redisTemplate.opsForValue().get(key(itemId));
		} catch (RuntimeException exception) {
			count(RESULT_ERROR);
			count("fallback");
			log.warn("Could not read item detail cache for item {}", itemId, exception);
			return Optional.empty();
		} finally {
			sample.stop(meterRegistry.timer(OPERATION_TIMER, "operation", "get"));
		}
		if (payload == null) {
			count("miss");
			return Optional.empty();
		}

		try {
			ItemDetailCacheValue value = objectMapper.readValue(payload, ItemDetailCacheValue.class);
			if (value == null) {
				count("miss");
				return Optional.empty();
			}
			count("hit");
			return Optional.of(value);
		} catch (JacksonException exception) {
			count(RESULT_ERROR);
			count("fallback");
			log.warn("Could not deserialize item detail cache for item {}", itemId, exception);
			return Optional.empty();
		}
	}

	public void put(Long itemId, ItemDetailCacheValue value) {
		String payload;
		try {
			payload = objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			count(RESULT_ERROR);
			log.warn("Could not serialize item detail cache for item {}", itemId, exception);
			return;
		}

		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			redisTemplate.opsForValue().set(key(itemId), payload, TTL);
		} catch (RuntimeException exception) {
			count(RESULT_ERROR);
			log.warn("Could not write item detail cache for item {}", itemId, exception);
		} finally {
			sample.stop(meterRegistry.timer(OPERATION_TIMER, "operation", "set"));
		}
	}

	public void evictAfterCommit(Collection<Long> itemIds) {
		List<Long> ids = itemIds.stream().distinct().toList();
		if (ids.isEmpty()) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			evict(ids);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				evict(ids);
			}
		});
	}

	private void evict(List<Long> itemIds) {
		itemIds.forEach(this::evict);
	}

	private void evict(Long itemId) {
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			redisTemplate.delete(key(itemId));
		} catch (RuntimeException exception) {
			count(RESULT_ERROR);
			log.warn("Could not evict item detail cache for item {}", itemId, exception);
		} finally {
			sample.stop(meterRegistry.timer(OPERATION_TIMER, "operation", "delete"));
		}
	}

	private void count(String result) {
		meterRegistry.counter(RESULT_COUNTER, "result", result).increment();
	}

	private static String key(Long itemId) {
		return KEY_PREFIX + itemId;
	}
}

package com.example.KTB_Agile_backend.item.cache;

import com.example.KTB_Agile_backend.item.entity.ItemState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisItemDetailCacheTest {
	private static final String KEY = "item:detail:core:123";
	private static final String JSON = "{\"title\":\"title\"}";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private ObjectMapper objectMapper;

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private RedisItemDetailCache cache;

	@BeforeEach
	void setUp() {
		cache = new RedisItemDetailCache(redisTemplate, objectMapper, meterRegistry);
	}

	@AfterEach
	void tearDown() {
		meterRegistry.close();
	}

	@Test
	void readsAndDeserializesCachedValue() throws Exception {
		ItemDetailCacheValue value = value();
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn(JSON);
		when(objectMapper.readValue(JSON, ItemDetailCacheValue.class)).thenReturn(value);

		Optional<ItemDetailCacheValue> result = cache.get(123L);

		assertThat(result).contains(value);
		assertThat(meterRegistry.get("item.detail.cache").tag("result", "hit").counter().count())
				.isEqualTo(1);
	}

	@Test
	void writesJsonStringWithFiveMinuteTtl() throws Exception {
		ItemDetailCacheValue value = value();
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(objectMapper.writeValueAsString(value)).thenReturn(JSON);

		cache.put(123L, value);

		verify(valueOperations).set(KEY, JSON, Duration.ofMinutes(5));
	}

	@Test
	void fallsBackWhenRedisReadFails() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenThrow(new IllegalStateException("Redis unavailable"));

		assertThat(cache.get(123L)).isEmpty();
		assertThat(meterRegistry.get("item.detail.cache").tag("result", "fallback").counter().count())
				.isEqualTo(1);
	}

	@Test
	void evictsOnlyAfterTransactionCommit() {
		TransactionSynchronizationManager.initSynchronization();
		try {
			cache.evictAfterCommit(List.of(123L));
			verify(redisTemplate, never()).delete(KEY);

			TransactionSynchronizationManager.getSynchronizations()
					.forEach(TransactionSynchronization::afterCommit);

			verify(redisTemplate).delete(KEY);
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private static ItemDetailCacheValue value() {
		return new ItemDetailCacheValue(
				42L,
				"title",
				"content",
				1,
				ItemState.AVAILABLE,
				456L,
				LocalDateTime.of(2026, 11, 11, 12, 0),
				LocalDateTime.of(2026, 11, 11, 12, 0),
				new BigDecimal("0.50"),
				new BigDecimal("0.30")
		);
	}
}

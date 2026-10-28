package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.dto.response.ItemRecommendationPageResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRecommendationServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ItemRepository itemRepository;

	@Mock
	private ItemQueryService itemQueryService;

	@Test
	void returnsItemSummariesInRedisRankOrderWithNextCursor() {
		ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>(List.of(
				new DefaultTypedTuple<>("300", 3.0),
				new DefaultTypedTuple<>("200", 8.0),
				new DefaultTypedTuple<>("100", 12.0)
		));
		when(zSetOperations.reverseRangeWithScores("item:recommend:{views}:popularity", 0, 2))
				.thenReturn(tuples);

		List<ItemSummaryProjection> projections = List.of(projection(200L), projection(100L));
		when(itemRepository.findActiveItemSummariesByIds(List.of(100L, 200L), UserStatus.ACTIVE))
				.thenReturn(projections);
		when(itemQueryService.toSummariesForUser(42L, projections))
				.thenReturn(List.of(summary(200L), summary(100L)));

		ItemRecommendationPageResponse response = service().findPopularItems(42L, "2", null);

		assertThat(response.items()).extracting(ItemSummary::itemId).containsExactly(100L, 200L);
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo(CursorCodec.encodeId(2L));
		verify(zSetOperations).reverseRangeWithScores("item:recommend:{views}:popularity", 0, 2);
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void recordsOneEventWithAnAtomicHashAndSortedSetScript() {
		long timestamp = System.currentTimeMillis();
		long bucketStart = Math.floorDiv(timestamp, 300_000L) * 300_000L;
		ItemRecommendationService service = service();

		service.recordView(100L, timestamp, 3, 15L);

		ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass(RedisScript.class);
		verify(redisTemplate).execute(
				script.capture(),
				eq(List.of("item:recommend:{views}:bucket:" + bucketStart,
						"item:recommend:{views}:popularity",
						"item:recommend:{views}:processed-offsets",
						"item:recommend:{views}:last-expired-bucket")),
				eq("100"), eq("3"), eq("15"), anyString(), eq("21600000"), anyString()
		);
		assertThat(script.getValue().getScriptAsString())
				.contains("redis.call('HINCRBY', KEYS[1], ARGV[1], 1)")
				.contains("redis.call('ZINCRBY', KEYS[2], 1, ARGV[1])")
				.contains("redis.call('HSET', KEYS[3], ARGV[2], ARGV[3])");
	}

	@Test
	void skipsEventsOlderThanTheSixHourWindow() {
		service().recordView(100L, System.currentTimeMillis() - 6 * 60 * 60 * 1000L - 1, 0, 1L);

		verifyNoInteractions(redisTemplate);
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void expiresBucketsBySubtractingHashCountsAndDeletingHashInOneScript() {
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		long now = System.currentTimeMillis();
		long cutoffBucket = Math.floorDiv(now - 21_600_000L, 300_000L) * 300_000L;
		long previousExpiredBucket = cutoffBucket - 2 * 300_000L;
		when(valueOperations.get("item:recommend:{views}:last-expired-bucket"))
				.thenReturn(String.valueOf(previousExpiredBucket));
		when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L);

		service().expireBuckets();

		ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass(RedisScript.class);
		verify(redisTemplate, atLeastOnce()).execute(script.capture(), anyList(), any(), any());
		assertThat(script.getValue().getScriptAsString())
				.contains("redis.call('HGETALL', KEYS[1])")
				.contains("tonumber(score) - tonumber(entries[index + 1])")
				.contains("redis.call('ZREM', KEYS[2], entries[index])")
				.contains("redis.call('DEL', KEYS[1])");
	}

	private ItemRecommendationService service() {
		return new ItemRecommendationService(redisTemplate, itemRepository, itemQueryService);
	}

	private static ItemSummaryProjection projection(Long itemId) {
		return new ItemSummaryProjection(
				itemId, "title", "content", 1, 42L, "owner", ItemState.AVAILABLE, LocalDateTime.now());
	}

	private static ItemSummary summary(Long itemId) {
		return new ItemSummary(
				itemId, "title", "content", 1, new ItemSummary.Owner(42L, "owner"),
				ItemState.AVAILABLE, null, 0L, 0L, 0L, false, LocalDateTime.now());
	}
}

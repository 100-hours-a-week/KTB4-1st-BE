package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.item.dto.projection.ItemSummaryProjection;
import com.example.KTB_Agile_backend.item.dto.response.ItemRecommendationPageResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemRecommendationService {

	// shortcut: five-minute buckets can retain events for up to five extra minutes at the window edge.
	private static final long BUCKET_INTERVAL_MILLIS = 5 * 60 * 1000L;
	private static final long WINDOW_MILLIS = 6 * 60 * 60 * 1000L;
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String BUCKET_KEY_PREFIX = "item:recommend:{views}:bucket:";
	private static final String POPULARITY_KEY = "item:recommend:{views}:popularity";
	// shortcut: clear this fence when intentionally replaying the recommendation group from an older offset.
	private static final String PROCESSED_OFFSETS_KEY = "item:recommend:{views}:processed-offsets";
	private static final String LAST_EXPIRED_BUCKET_KEY = "item:recommend:{views}:last-expired-bucket";
	private static final DefaultRedisScript<Long> RECORD_VIEW = new DefaultRedisScript<>("""
			local bucketType = redis.call('TYPE', KEYS[1]).ok
			local popularityType = redis.call('TYPE', KEYS[2]).ok
			local offsetsType = redis.call('TYPE', KEYS[3]).ok
			local markerType = redis.call('TYPE', KEYS[4]).ok
			if (bucketType ~= 'none' and bucketType ~= 'hash')
				or (popularityType ~= 'none' and popularityType ~= 'zset')
				or (offsetsType ~= 'none' and offsetsType ~= 'hash')
				or (markerType ~= 'none' and markerType ~= 'string') then
				return redis.error_reply('Unexpected Redis key type for item recommendation')
			end
			local now = redis.call('TIME')
			local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
			if tonumber(ARGV[4]) < nowMillis - tonumber(ARGV[5]) then
				return 0
			end
			local lastExpired = redis.call('GET', KEYS[4])
			local lastOffset = redis.call('HGET', KEYS[3], ARGV[2])
			if lastOffset and tonumber(ARGV[3]) <= tonumber(lastOffset) then
				return 0
			end
			redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
			redis.call('ZINCRBY', KEYS[2], 1, ARGV[1])
			redis.call('HSET', KEYS[3], ARGV[2], ARGV[3])
			if not lastExpired then
				redis.call('SET', KEYS[4], ARGV[6])
			end
			return 1
			""", Long.class);
	private static final DefaultRedisScript<Long> EXPIRE_BUCKET = new DefaultRedisScript<>("""
			local bucketType = redis.call('TYPE', KEYS[1]).ok
			local popularityType = redis.call('TYPE', KEYS[2]).ok
			local markerType = redis.call('TYPE', KEYS[3]).ok
			if (bucketType ~= 'none' and bucketType ~= 'hash')
				or (popularityType ~= 'none' and popularityType ~= 'zset')
				or (markerType ~= 'none' and markerType ~= 'string') then
				return redis.error_reply('Unexpected Redis key type for item recommendation')
			end
			if redis.call('GET', KEYS[3]) ~= ARGV[1] then
				return 0
			end
			local entries = redis.call('HGETALL', KEYS[1])
			for index = 1, #entries, 2 do
				local score = redis.call('ZSCORE', KEYS[2], entries[index])
				if score then
					local remaining = tonumber(score) - tonumber(entries[index + 1])
					if remaining <= 0 then
						redis.call('ZREM', KEYS[2], entries[index])
					else
						redis.call('ZADD', KEYS[2], remaining, entries[index])
					end
				end
			end
			redis.call('DEL', KEYS[1])
			redis.call('SET', KEYS[3], ARGV[2])
			return 1
			""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final ItemRepository itemRepository;
	private final ItemQueryService itemQueryService;

	public void recordView(Long itemId, long timestampMillis, int partition, long offset) {
		long nowMillis = System.currentTimeMillis();
		if (timestampMillis < nowMillis - WINDOW_MILLIS) {
			return;
		}
		long bucketStart = Math.floorDiv(timestampMillis, BUCKET_INTERVAL_MILLIS) * BUCKET_INTERVAL_MILLIS;
		long initialExpiredBucket = Math.floorDiv(
				nowMillis - WINDOW_MILLIS, BUCKET_INTERVAL_MILLIS) * BUCKET_INTERVAL_MILLIS
				- BUCKET_INTERVAL_MILLIS;
		redisTemplate.execute(
				RECORD_VIEW,
				List.of(BUCKET_KEY_PREFIX + bucketStart, POPULARITY_KEY, PROCESSED_OFFSETS_KEY,
						LAST_EXPIRED_BUCKET_KEY),
				itemId.toString(), String.valueOf(partition), String.valueOf(offset),
				String.valueOf(timestampMillis), String.valueOf(WINDOW_MILLIS),
				String.valueOf(initialExpiredBucket)
		);
	}

	@Scheduled(fixedDelay = BUCKET_INTERVAL_MILLIS)
	public void expireBuckets() {
		long cutoffBucket = Math.floorDiv(
				System.currentTimeMillis() - WINDOW_MILLIS, BUCKET_INTERVAL_MILLIS) * BUCKET_INTERVAL_MILLIS;
		long expireThrough = cutoffBucket - BUCKET_INTERVAL_MILLIS;
		String lastExpired = redisTemplate.opsForValue().get(LAST_EXPIRED_BUCKET_KEY);
		if (lastExpired == null) {
			return;
		}

		long lastExpiredBucket = Long.parseLong(lastExpired);
		while (lastExpiredBucket < expireThrough) {
			long bucketToExpire = lastExpiredBucket + BUCKET_INTERVAL_MILLIS;
			Long expired = redisTemplate.execute(
					EXPIRE_BUCKET,
					List.of(BUCKET_KEY_PREFIX + bucketToExpire, POPULARITY_KEY, LAST_EXPIRED_BUCKET_KEY),
					String.valueOf(lastExpiredBucket), String.valueOf(bucketToExpire)
			);
			if (Long.valueOf(1).equals(expired)) {
				lastExpiredBucket = bucketToExpire;
			} else {
				String latestExpired = redisTemplate.opsForValue().get(LAST_EXPIRED_BUCKET_KEY);
				if (latestExpired == null || Long.parseLong(latestExpired) <= lastExpiredBucket) {
					return;
				}
				lastExpiredBucket = Long.parseLong(latestExpired);
			}
		}
	}

	@Transactional(readOnly = true)
	public ItemRecommendationPageResponse findPopularItems(Long userId, String sizeValue, String cursor) {
		int size = parseSize(sizeValue);
		long offset = cursor == null || cursor.isBlank() ? 0 : CursorCodec.decodeId(cursor);
		long end;
		try {
			end = Math.addExact(offset, size);
		} catch (ArithmeticException exception) {
			throw new ApiException(ErrorCode.INVALID_CURSOR, List.of(), exception);
		}

		Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
				.reverseRangeWithScores(POPULARITY_KEY, offset, end);
		if (tuples == null || tuples.isEmpty()) {
			return new ItemRecommendationPageResponse(List.of(), null, false);
		}

		List<ZSetOperations.TypedTuple<String>> ranked = new ArrayList<>(tuples);
		ranked.sort((left, right) -> {
			int scoreOrder = Double.compare(right.getScore(), left.getScore());
			return scoreOrder == 0 ? right.getValue().compareTo(left.getValue()) : scoreOrder;
		});
		boolean hasNext = ranked.size() > size;
		List<ZSetOperations.TypedTuple<String>> page = hasNext ? ranked.subList(0, size) : ranked;
		List<Long> itemIds = page.stream().map(tuple -> Long.valueOf(tuple.getValue())).toList();

		List<ItemSummaryProjection> summaries = itemRepository.findActiveItemSummariesByIds(
				itemIds, UserStatus.ACTIVE);
		Map<Long, ItemSummary> summariesById = new LinkedHashMap<>();
		itemQueryService.toSummariesForUser(userId, summaries)
				.forEach(summary -> summariesById.put(summary.itemId(), summary));

		List<ItemSummary> items = itemIds.stream()
				.filter(summariesById::containsKey)
				.map(summariesById::get)
				.toList();
		// shortcut: rank cursors can shift between requests as new views change scores.
		String nextCursor = hasNext ? CursorCodec.encodeId(offset + size) : null;
		return new ItemRecommendationPageResponse(items, nextCursor, hasNext);
	}

	private static int parseSize(String value) {
		try {
			int size = Integer.parseInt(value == null ? String.valueOf(DEFAULT_PAGE_SIZE) : value);
			if (size >= 1 && size <= MAX_PAGE_SIZE) {
				return size;
			}
		} catch (NumberFormatException ignored) {
		}
		throw new ApiException(ErrorCode.BAD_REQUEST, "size 값이 올바르지 않습니다.");
	}
}

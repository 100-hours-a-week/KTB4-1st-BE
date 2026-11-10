package com.example.KTB_Agile_backend.search.service;

import com.example.KTB_Agile_backend.search.dto.response.SearchRankingEntry;
import com.example.KTB_Agile_backend.search.dto.response.SearchRankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchRankingService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int TOP_RANKINGS = 10;
	private static final long RETENTION_DAYS = 7;
	private static final String KEY_PREFIX = "search:rank:daily:";
	private static final DefaultRedisScript<Long> RECORD_SEARCH = new DefaultRedisScript<>("""
			local rankingType = redis.call('TYPE', KEYS[1]).ok
			local offsetsType = redis.call('TYPE', KEYS[2]).ok
			if (rankingType ~= 'none' and rankingType ~= 'zset')
				or (offsetsType ~= 'none' and offsetsType ~= 'hash') then
				return redis.error_reply('Unexpected Redis key type for search ranking')
			end
			local now = redis.call('TIME')
			if tonumber(ARGV[4]) <= tonumber(now[1]) then
				return 0
			end
			local lastOffset = redis.call('HGET', KEYS[2], ARGV[2])
			if lastOffset and tonumber(ARGV[3]) <= tonumber(lastOffset) then
				return 0
			end
			redis.call('ZINCRBY', KEYS[1], 1, ARGV[1])
			redis.call('HSET', KEYS[2], ARGV[2], ARGV[3])
			redis.call('EXPIREAT', KEYS[1], ARGV[4])
			redis.call('EXPIREAT', KEYS[2], ARGV[4])
			return 1
			""", Long.class);

	private final StringRedisTemplate redisTemplate;

	public void recordSearch(String keyword, long timestampMillis, int partition, long offset) {
		LocalDate date = Instant.ofEpochMilli(timestampMillis).atZone(SEOUL).toLocalDate();
		long expiresAt = date.plusDays(RETENTION_DAYS).atStartOfDay(SEOUL).toEpochSecond();
		String dateKey = date.format(KEY_DATE_FORMAT);
		String keyPrefix = KEY_PREFIX + "{" + dateKey + "}:";

		redisTemplate.execute(
				RECORD_SEARCH,
				List.of(keyPrefix + "ranking", keyPrefix + "processed-offsets"),
				keyword, String.valueOf(partition), String.valueOf(offset), String.valueOf(expiresAt)
		);
	}

	public SearchRankingResponse findToday() {
		LocalDate today = LocalDate.now(SEOUL);
		String dateKey = today.format(KEY_DATE_FORMAT);
		Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
				.reverseRangeWithScores(KEY_PREFIX + "{" + dateKey + "}:ranking", 0, TOP_RANKINGS - 1);
		if (tuples == null || tuples.isEmpty()) {
			return new SearchRankingResponse(today, List.of());
		}

		List<ZSetOperations.TypedTuple<String>> ranked = new ArrayList<>(tuples);
		ranked.sort((left, right) -> {
			int scoreOrder = Double.compare(right.getScore(), left.getScore());
			return scoreOrder == 0 ? right.getValue().compareTo(left.getValue()) : scoreOrder;
		});
		List<SearchRankingEntry> rankings = new ArrayList<>(ranked.size());
		for (int index = 0; index < ranked.size(); index++) {
			ZSetOperations.TypedTuple<String> tuple = ranked.get(index);
			rankings.add(new SearchRankingEntry(index + 1, tuple.getValue(), tuple.getScore().longValue()));
		}
		return new SearchRankingResponse(today, rankings);
	}
}

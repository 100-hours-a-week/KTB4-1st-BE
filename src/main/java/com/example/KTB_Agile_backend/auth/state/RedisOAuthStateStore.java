package com.example.KTB_Agile_backend.auth.state;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RedisOAuthStateStore {

	private static final String KEY_PREFIX = "auth:oauth:state:";
	private static final DefaultRedisScript<Long> CONSUME_IF_VALID = new DefaultRedisScript<>("""
			local provider = redis.call('GET', KEYS[1])
			if provider and provider == ARGV[1] then
				return redis.call('DEL', KEYS[1])
			end
			return 0
			""", Long.class);

	private final StringRedisTemplate redisTemplate;

	public void save(String stateHash, String provider, Duration ttl) {
		redisTemplate.opsForValue().set(KEY_PREFIX + stateHash, provider, ttl);
	}

	public boolean consumeIfValid(String stateHash, String provider) {
		return Long.valueOf(1L).equals(
				redisTemplate.execute(CONSUME_IF_VALID, List.of(KEY_PREFIX + stateHash), provider)
		);
	}
}

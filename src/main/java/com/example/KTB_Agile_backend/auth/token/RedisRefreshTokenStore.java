package com.example.KTB_Agile_backend.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_PREFIX = "auth:refresh:";
	private static final DefaultRedisScript<Long> CREATE_SESSION = new DefaultRedisScript<>("""
			if redis.call('EXISTS', KEYS[1]) == 1 then
				return 0
			end
			redis.call('HSET', KEYS[1],
				'user_id', ARGV[1],
				'current_hash', ARGV[2],
				'status', 'ACTIVE')
			redis.call('PEXPIRE', KEYS[1], ARGV[3])
			return 1
			""", Long.class);
	private static final DefaultRedisScript<String> FIND_CURRENT_USER = new DefaultRedisScript<>("""
			local userId = redis.call('HGET', KEYS[1], 'user_id')
			local currentHash = redis.call('HGET', KEYS[1], 'current_hash')
			local status = redis.call('HGET', KEYS[1], 'status')
			if not userId or status ~= 'ACTIVE' or redis.call('PTTL', KEYS[1]) <= 0 then
				return 'INVALID'
			end
			if currentHash == ARGV[1] then
				return 'OK:' .. userId
			end
			if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
				redis.call('HSET', KEYS[1], 'status', 'REVOKED')
				return 'REPLAYED'
			end
			return 'INVALID'
			""", String.class);
	private static final DefaultRedisScript<String> ROTATE = new DefaultRedisScript<>("""
			local userId = redis.call('HGET', KEYS[1], 'user_id')
			local currentHash = redis.call('HGET', KEYS[1], 'current_hash')
			local status = redis.call('HGET', KEYS[1], 'status')
			local ttl = redis.call('PTTL', KEYS[1])
			if not userId or status ~= 'ACTIVE' or ttl <= 0 then
				return 'INVALID'
			end
			if currentHash == ARGV[1] then
				redis.call('SADD', KEYS[2], ARGV[1])
				-- Keep replay markers until the session's original expiry.
				redis.call('PEXPIRE', KEYS[2], ttl)
				redis.call('HSET', KEYS[1], 'current_hash', ARGV[2])
				return 'OK:' .. userId .. ':' .. ttl
			end
			if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
				redis.call('HSET', KEYS[1], 'status', 'REVOKED')
				return 'REPLAYED'
			end
			return 'INVALID'
			""", String.class);
	private static final DefaultRedisScript<Long> REVOKE = new DefaultRedisScript<>("""
			local status = redis.call('HGET', KEYS[1], 'status')
			if status ~= 'ACTIVE' then
				return 0
			end
			local currentHash = redis.call('HGET', KEYS[1], 'current_hash')
			if currentHash == ARGV[1] or redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
				redis.call('HSET', KEYS[1], 'status', 'REVOKED')
				return 1
			end
			return 0
			""", Long.class);

	private final StringRedisTemplate redisTemplate;

	@Override
	public void create(String sessionId, Long userId, String tokenHash, Duration ttl) {
		Long created = redisTemplate.execute(
				CREATE_SESSION,
				List.of(sessionKey(sessionId)),
				String.valueOf(userId), tokenHash, String.valueOf(ttl.toMillis())
		);
		if (!Long.valueOf(1L).equals(created)) {
			throw new IllegalStateException("Refresh token session already exists");
		}
	}

	@Override
	public Optional<Long> findCurrentUserId(String sessionId, String tokenHash) {
		String result = redisTemplate.execute(
				FIND_CURRENT_USER,
				List.of(sessionKey(sessionId), usedTokensKey(sessionId)),
				tokenHash
		);
		if (result == null || !result.startsWith("OK:")) {
			return Optional.empty();
		}
		return Optional.of(Long.parseLong(result.substring(3)));
	}

	@Override
	public Rotation rotate(String sessionId, String oldTokenHash, String newTokenHash) {
		String result = redisTemplate.execute(
				ROTATE,
				List.of(sessionKey(sessionId), usedTokensKey(sessionId)),
				oldTokenHash, newTokenHash
		);
		if (result == null || !result.startsWith("OK:")) {
			return new Rotation(
					RotationStatus.INVALID,
					null,
					Duration.ZERO
			);
		}

		String[] values = result.split(":");
		return new Rotation(
				RotationStatus.ROTATED,
				Long.parseLong(values[1]),
				Duration.ofMillis(Long.parseLong(values[2]))
		);
	}

	@Override
	public void revoke(String sessionId, String tokenHash) {
		redisTemplate.execute(
				REVOKE,
				List.of(sessionKey(sessionId), usedTokensKey(sessionId)),
				tokenHash
		);
	}

	private static String sessionKey(String sessionId) {
		return KEY_PREFIX + "session:" + sessionId;
	}

	private static String usedTokensKey(String sessionId) {
		return KEY_PREFIX + "used:" + sessionId;
	}
}

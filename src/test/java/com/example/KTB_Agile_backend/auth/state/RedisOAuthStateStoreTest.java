package com.example.KTB_Agile_backend.auth.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisOAuthStateStoreTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void consumesStateWithOneAtomicProviderCheckAndDeleteScript() {
		String key = "auth:oauth:state:state-hash";
		when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq("KAKAO")))
				.thenReturn(1L);
		RedisOAuthStateStore store = new RedisOAuthStateStore(redisTemplate);

		assertThat(store.consumeIfValid("state-hash", "KAKAO")).isTrue();

		ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass(RedisScript.class);
		verify(redisTemplate).execute(script.capture(), eq(List.of(key)), eq("KAKAO"));
		assertThat(script.getValue().getScriptAsString())
				.contains("local provider = redis.call('GET', KEYS[1])")
				.contains("provider == ARGV[1]")
				.contains("return redis.call('DEL', KEYS[1])");
	}
}

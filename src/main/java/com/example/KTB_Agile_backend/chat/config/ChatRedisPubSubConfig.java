package com.example.KTB_Agile_backend.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ChatRedisPubSubConfig {

	@Bean
	RedisMessageListenerContainer chatMessageListenerContainer(
			RedisConnectionFactory connectionFactory
	) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		return container;
	}
}

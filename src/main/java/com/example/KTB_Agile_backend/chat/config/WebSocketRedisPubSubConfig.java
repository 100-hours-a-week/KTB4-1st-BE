package com.example.KTB_Agile_backend.chat.config;

import com.example.KTB_Agile_backend.chat.storage.ChatMessageStorageRouter;
import com.example.KTB_Agile_backend.notification.messaging.RedisPubSubNotificationBroker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class WebSocketRedisPubSubConfig {

	@Bean
	RedisMessageListenerContainer webSocketRedisMessageListenerContainer(
			RedisConnectionFactory connectionFactory,
			RedisPubSubNotificationBroker notificationBroker,
			ChatMessageStorageRouter chatMessageStorageRouter
	) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(notificationBroker,
				new ChannelTopic(RedisPubSubNotificationBroker.NOTIFICATION_CHANNEL));
		container.addMessageListener(chatMessageStorageRouter,
				new ChannelTopic(ChatMessageStorageRouter.ROUTE_CHANNEL));
		return container;
	}
}

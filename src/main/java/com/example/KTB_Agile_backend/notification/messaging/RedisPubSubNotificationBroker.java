package com.example.KTB_Agile_backend.notification.messaging;

import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisPubSubNotificationBroker implements NotificationMessagePublisher, MessageListener {

	public static final String NOTIFICATION_CHANNEL = "notifications:users";
	private static final String USER_QUEUE_DESTINATION = "/queue/notifications";

	private final StringRedisTemplate redisTemplate;
	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	public RedisPubSubNotificationBroker(
			StringRedisTemplate redisTemplate,
			@Lazy SimpMessagingTemplate messagingTemplate,
			ObjectMapper objectMapper
	) {
		this.redisTemplate = redisTemplate;
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publish(Long userId, NotificationMessage notification) {
		if (userId == null || userId < 1 || notification == null) {
			throw new IllegalArgumentException("A valid user ID and notification are required");
		}
		redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, toJson(new RedisNotificationEvent(userId, notification)));
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		RedisNotificationEvent event = fromJson(message.getBody());
		if (event.userId() != null && event.userId() > 0 && event.notification() != null) {
			messagingTemplate.convertAndSendToUser(
					event.userId().toString(), USER_QUEUE_DESTINATION, event.notification());
		}
	}

	private String toJson(RedisNotificationEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Unable to serialize notification", exception);
		}
	}

	private RedisNotificationEvent fromJson(byte[] payload) {
		try {
			return objectMapper.readValue(payload, RedisNotificationEvent.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize notification", exception);
		}
	}
}

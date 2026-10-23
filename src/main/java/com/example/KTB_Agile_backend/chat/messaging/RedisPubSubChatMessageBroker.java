package com.example.KTB_Agile_backend.chat.messaging;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisPubSubChatMessageBroker implements ChatMessagePublisher, MessageListener {

	public static final String ROOM_CHANNEL_PREFIX = "chat:rooms:";
	private static final String ROOM_TOPIC_PREFIX = "/topic/chat/rooms/";

	private final StringRedisTemplate redisTemplate;
	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	public RedisPubSubChatMessageBroker(
			StringRedisTemplate redisTemplate,
			@Lazy SimpMessagingTemplate messagingTemplate,
			ObjectMapper objectMapper
	) {
		this.redisTemplate = redisTemplate;
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publish(ChatMessageResponse message) {
		redisTemplate.convertAndSend(ROOM_CHANNEL_PREFIX + message.chatRoomId(), toJson(message));
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		ChatMessageResponse response = fromJson(message.getBody());
		messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + response.chatRoomId(), response);
	}

	private String toJson(ChatMessageResponse message) {
		try {
			return objectMapper.writeValueAsString(message);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Unable to serialize chat message", exception);
		}
	}

	private ChatMessageResponse fromJson(byte[] payload) {
		try {
			return objectMapper.readValue(payload, ChatMessageResponse.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Unable to deserialize chat message", exception);
		}
	}
}

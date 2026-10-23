package com.example.KTB_Agile_backend.chat.messaging;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPubSubChatMessageBrokerTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private Message redisMessage;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final ChatMessageResponse response = new ChatMessageResponse(
			301L, 42L, 7L, "hello", ChatMessageType.TEXT, null, null, null, null, null);

	@Test
	void publishesJsonToTheRoomChannel() throws Exception {
		RedisPubSubChatMessageBroker broker =
				new RedisPubSubChatMessageBroker(redisTemplate, messagingTemplate, objectMapper);

		broker.publish(response);

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(redisTemplate).convertAndSend(eq("chat:rooms:42"), payload.capture());
		assertThat(objectMapper.readValue(payload.getValue(), ChatMessageResponse.class)).isEqualTo(response);
	}

	@Test
	void forwardsRedisMessagesToTheMatchingLocalStompTopic() throws Exception {
		RedisPubSubChatMessageBroker broker =
				new RedisPubSubChatMessageBroker(redisTemplate, messagingTemplate, objectMapper);
		when(redisMessage.getBody()).thenReturn(
				objectMapper.writeValueAsBytes(response));

		broker.onMessage(redisMessage, null);

		verify(messagingTemplate).convertAndSend("/topic/chat/rooms/42", response);
	}
}

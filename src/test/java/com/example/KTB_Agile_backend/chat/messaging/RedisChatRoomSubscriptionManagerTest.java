package com.example.KTB_Agile_backend.chat.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisChatRoomSubscriptionManagerTest {

	@Mock
	private RedisMessageListenerContainer listenerContainer;

	@Mock
	private RedisPubSubChatMessageBroker chatMessageBroker;

	@Test
	void subscribesRedisOncePerRoomAndUnsubscribesAfterLastLocalClientLeaves() {
		RedisChatRoomSubscriptionManager manager =
				new RedisChatRoomSubscriptionManager(listenerContainer, chatMessageBroker);
		MessageChannel channel = mock(MessageChannel.class);
		MessageHandler simpleBroker = mock(SimpleBrokerMessageHandler.class);

		handle(manager, channel, simpleBroker, StompCommand.SUBSCRIBE, "session-1", "sub-1");
		handle(manager, channel, simpleBroker, StompCommand.SUBSCRIBE, "session-2", "sub-2");

		ArgumentCaptor<ChannelTopic> addedTopic = ArgumentCaptor.forClass(ChannelTopic.class);
		verify(listenerContainer).addMessageListener(same(chatMessageBroker), addedTopic.capture());
		assertThat(addedTopic.getValue().getTopic()).isEqualTo("chat:rooms:42");

		handle(manager, channel, simpleBroker, StompCommand.UNSUBSCRIBE, "session-1", "sub-1");
		verify(listenerContainer, never()).removeMessageListener(
				same(chatMessageBroker), org.mockito.ArgumentMatchers.any(ChannelTopic.class));

		handle(manager, channel, simpleBroker, StompCommand.UNSUBSCRIBE, "session-2", "sub-2");
		ArgumentCaptor<ChannelTopic> removedTopic = ArgumentCaptor.forClass(ChannelTopic.class);
		verify(listenerContainer, times(1)).removeMessageListener(same(chatMessageBroker), removedTopic.capture());
		assertThat(removedTopic.getValue().getTopic()).isEqualTo("chat:rooms:42");
	}

	private void handle(
			RedisChatRoomSubscriptionManager manager,
			MessageChannel channel,
			MessageHandler handler,
			StompCommand command,
			String sessionId,
			String subscriptionId
	) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		accessor.setDestination("/topic/chat/rooms/42");
		Message<String> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());
		manager.afterMessageHandled(message, channel, handler, null);
	}
}

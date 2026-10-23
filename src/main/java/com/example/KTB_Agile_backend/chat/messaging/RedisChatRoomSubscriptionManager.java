package com.example.KTB_Agile_backend.chat.messaging;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class RedisChatRoomSubscriptionManager implements ExecutorChannelInterceptor {

	private static final String ROOM_TOPIC_PREFIX = "/topic/chat/rooms/";

	private final RedisMessageListenerContainer listenerContainer;
	private final RedisPubSubChatMessageBroker chatMessageBroker;
	private final Map<String, Map<String, Long>> roomBySessionSubscription = new HashMap<>();
	private final Map<Long, Integer> subscriptionCountByRoom = new HashMap<>();

	public RedisChatRoomSubscriptionManager(
			@Qualifier("chatMessageListenerContainer") RedisMessageListenerContainer listenerContainer,
			RedisPubSubChatMessageBroker chatMessageBroker
	) {
		this.listenerContainer = listenerContainer;
		this.chatMessageBroker = chatMessageBroker;
	}

	@Override
	public void afterMessageHandled(
			Message<?> message,
			MessageChannel channel,
			MessageHandler handler,
			Exception exception
	) {
		if (exception != null || !(handler instanceof SimpleBrokerMessageHandler)) {
			return;
		}

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getSessionId() == null) {
			return;
		}

		synchronized (this) {
			if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
				subscribe(accessor);
			} else if (StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
				unsubscribe(accessor.getSessionId(), accessor.getSubscriptionId());
			}
		}
	}

	@EventListener
	public synchronized void onSessionDisconnect(SessionDisconnectEvent event) {
		Map<String, Long> subscriptions = roomBySessionSubscription.remove(event.getSessionId());
		if (subscriptions != null) {
			subscriptions.values().forEach(this::removeRoomSubscription);
		}
	}

	private void subscribe(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		String sessionId = accessor.getSessionId();
		String subscriptionId = accessor.getSubscriptionId();
		if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)
				|| sessionId == null || subscriptionId == null) {
			return;
		}

		long roomId = Long.parseLong(destination.substring(ROOM_TOPIC_PREFIX.length()));
		Map<String, Long> subscriptions = roomBySessionSubscription.computeIfAbsent(sessionId,
				ignored -> new HashMap<>());
		if (subscriptions.containsKey(subscriptionId)) {
			return;
		}

		int currentCount = subscriptionCountByRoom.getOrDefault(roomId, 0);
		if (currentCount == 0) {
			listenerContainer.addMessageListener(chatMessageBroker,
					new ChannelTopic(RedisPubSubChatMessageBroker.ROOM_CHANNEL_PREFIX + roomId));
		}
		subscriptions.put(subscriptionId, roomId);
		subscriptionCountByRoom.put(roomId, currentCount + 1);
	}

	private void unsubscribe(String sessionId, String subscriptionId) {
		if (subscriptionId == null) {
			return;
		}
		Map<String, Long> subscriptions = roomBySessionSubscription.get(sessionId);
		if (subscriptions == null) {
			return;
		}

		Long roomId = subscriptions.remove(subscriptionId);
		if (subscriptions.isEmpty()) {
			roomBySessionSubscription.remove(sessionId);
		}
		if (roomId != null) {
			removeRoomSubscription(roomId);
		}
	}

	private void removeRoomSubscription(Long roomId) {
		int remainingCount = subscriptionCountByRoom.getOrDefault(roomId, 0) - 1;
		if (remainingCount <= 0) {
			subscriptionCountByRoom.remove(roomId);
			listenerContainer.removeMessageListener(chatMessageBroker,
					new ChannelTopic(RedisPubSubChatMessageBroker.ROOM_CHANNEL_PREFIX + roomId));
		} else {
			subscriptionCountByRoom.put(roomId, remainingCount);
		}
	}
}

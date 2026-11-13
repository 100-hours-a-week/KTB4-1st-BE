package com.example.KTB_Agile_backend.chat.storage;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ChatMessageStorageRouter implements MessageListener {
	public static final String ROUTE_CHANNEL = "chat:message:storage-route";

	private final ObjectMapper objectMapper;
	private volatile Route route = new Route(Store.MYSQL, Store.MYSQL);

	public ChatMessageStorageRouter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public boolean readsFromMongo() {
		return route.read() == Store.MONGODB;
	}

	public boolean writesToMongo() {
		return route.write() == Store.MONGODB;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			RouteUpdate update = objectMapper.readValue(message.getBody(), RouteUpdate.class);
			route = new Route(Store.valueOf(update.read()), Store.valueOf(update.write()));
		} catch (JacksonException | IllegalArgumentException | NullPointerException ignored) {
			// Ignore invalid route messages and keep the last valid route.
		}
	}

	private enum Store {
		MYSQL,
		MONGODB
	}

	private record RouteUpdate(String read, String write) {
	}

	private record Route(Store read, Store write) {
	}
}

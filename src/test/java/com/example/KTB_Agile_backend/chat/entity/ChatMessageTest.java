package com.example.KTB_Agile_backend.chat.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ChatMessageTest {

	@Test
	void userMessageRequiresUser() {
		assertThrows(NullPointerException.class,
				() -> new ChatMessage(22L, null, "메시지"));
	}

	@Test
	void systemMessageHasNoUserAndSystemType() {
		ChatMessage message = ChatMessage.system(
				22L, ChatSystemEventType.EXCHANGE_REQUEST_REJECTED);

		assertThat(message.getChatRoomId()).isEqualTo(22L);
		assertThat(message.getUserId()).isNull();
		assertThat(message.getMessageType()).isEqualTo(ChatMessageType.SYSTEM);
		assertThat(message.getSystemEventType()).isEqualTo(ChatSystemEventType.EXCHANGE_REQUEST_REJECTED);
		assertThat(message.getContent()).isEqualTo("교환이 거절 되었습니다.");
	}

	@Test
	void userMessageHasUserAndTextType() {
		ChatMessage message = new ChatMessage(22L, 42L, "메시지");

		assertThat(message.getChatRoomId()).isEqualTo(22L);
		assertThat(message.getUserId()).isEqualTo(42L);
		assertThat(message.getMessageType()).isEqualTo(ChatMessageType.TEXT);
	}
}

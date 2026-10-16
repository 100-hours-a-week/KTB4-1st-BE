package com.example.KTB_Agile_backend.chat.entity;

import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ChatMessageTest {

	@Test
	void userMessageRequiresUser() {
		assertThrows(NullPointerException.class,
				() -> new ChatMessage(mock(ChatRoom.class), null, "메시지"));
	}

	@Test
	void systemMessageHasNoUserAndSystemType() {
		ChatMessage message = ChatMessage.system(mock(ChatRoom.class), "시스템 메시지");

		assertThat(message.getUser()).isNull();
		assertThat(message.getMessageType()).isEqualTo(ChatMessageType.SYSTEM);
	}

	@Test
	void userMessageHasUserAndTextType() {
		User user = new User("사용자");
		ChatMessage message = new ChatMessage(mock(ChatRoom.class), user, "메시지");

		assertThat(message.getUser()).isSameAs(user);
		assertThat(message.getMessageType()).isEqualTo(ChatMessageType.TEXT);
	}
}

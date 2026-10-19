package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import com.example.KTB_Agile_backend.chat.entity.ChatRoom;
import com.example.KTB_Agile_backend.chat.entity.ChatSystemEventType;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ChatMemberRepository chatMemberRepository;

	@Mock
	private ChatMessageService chatMessageService;

	@InjectMocks
	private ChatRoomService service;

	@Test
	void leavingRoomAddsSystemMessageForOtherMember() {
		Long chatRoomId = 22L;
		Long userId = 42L;
		ChatRoom chatRoom = mock(ChatRoom.class);
		ChatMember member = mock(ChatMember.class);
		when(chatRoomRepository.existsById(chatRoomId)).thenReturn(true);
		when(chatMemberRepository.findActiveMember(chatRoomId, userId)).thenReturn(Optional.of(member));
		when(member.getChatRoom()).thenReturn(chatRoom);

		service.leave(chatRoomId, userId);

		verify(chatMessageService).sendSystemMessage(chatRoom, ChatSystemEventType.CHAT_ROOM_MEMBER_LEFT);
		verify(member).leave(any());
		verify(chatRoom).close();
		assertThat(ChatSystemEventType.CHAT_ROOM_MEMBER_LEFT.content())
				.isEqualTo("상대방이 채팅방을 나갔습니다");
	}
}

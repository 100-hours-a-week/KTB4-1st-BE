package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatMessageType;
import com.example.KTB_Agile_backend.chat.entity.ChatRoom;
import com.example.KTB_Agile_backend.chat.entity.ChatSystemEventType;
import com.example.KTB_Agile_backend.chat.messaging.ChatMessagePublisher;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatRoomRepository;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

	@Mock
	private ChatMemberRepository chatMemberRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ExchangeRequestRepository exchangeRequestRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private GroupMemberRepository groupMemberRepository;

	@Mock
	private ChatMessagePublisher chatMessagePublisher;

	@Mock
	private ChatMessageResponseMapper chatMessageResponseMapper;

	@InjectMocks
	private ChatMessageService service;

	@Test
	void persistsSystemMessageAndBroadcastsOnlyAfterCommit() {
		ChatRoom chatRoom = mock(ChatRoom.class);
		when(chatRoom.getId()).thenReturn(22L);
		ChatMessage message = mock(ChatMessage.class);
		ChatSystemEventType eventType = ChatSystemEventType.EXCHANGE_REQUEST_CANCELED;
		LocalDateTime createdAt = LocalDateTime.of(2026, 10, 18, 12, 30);
		when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);
		when(message.getCreatedAt()).thenReturn(createdAt);
		ChatMessageResponse response = new ChatMessageResponse(301L, 22L, null, eventType.content(),
				ChatMessageType.SYSTEM, eventType, createdAt, null, null, null);
		when(chatMessageResponseMapper.findSystemMessageThumbnails(any(), eq(null))).thenReturn(java.util.Map.of());
		when(chatMessageResponseMapper.toResponse(message, null, null, null, java.util.Map.of())).thenReturn(response);

		TransactionSynchronizationManager.initSynchronization();
		try {
			service.sendSystemMessage(chatRoom, eventType);

			verifyNoInteractions(chatMessagePublisher);
			List<TransactionSynchronization> synchronizations =
					TransactionSynchronizationManager.getSynchronizations();
			synchronizations.forEach(TransactionSynchronization::afterCommit);

			verify(chatMessageRepository).save(any(ChatMessage.class));
			verify(chatRoom).updateLastMessageAt(createdAt);
			verify(chatMessagePublisher).publish(response);
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}
}

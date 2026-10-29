package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatRoom;
import com.example.KTB_Agile_backend.chat.entity.ChatRoomStatus;
import com.example.KTB_Agile_backend.chat.entity.ChatSystemEventType;
import com.example.KTB_Agile_backend.chat.messaging.ChatMessagePublisher;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatRoomRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

	private static final int MAX_CONTENT_LENGTH = 2000;

	private final ChatMemberRepository chatMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ExchangeRequestRepository exchangeRequestRepository;
	private final UserRepository userRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ChatMessagePublisher chatMessagePublisher;
	private final ChatMessageResponseMapper chatMessageResponseMapper;

	@Transactional
	public ChatMessageResponse send(Long chatRoomId, Long senderId, String content) {
		if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("메시지는 1~2000자여야 합니다.");
		}

		ChatMember member = chatMemberRepository
				.findActiveMember(chatRoomId, senderId)
				.orElseThrow(() -> new AccessDeniedException("채팅방 멤버만 메시지를 보낼 수 있습니다."));
		ChatRoom chatRoom = chatRoomRepository.findById(member.getChatRoomId())
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
		if (chatRoom.isDeleted() || chatRoom.getChatRoomStatus() != ChatRoomStatus.OPEN) {
			throw new AccessDeniedException("현재 채팅방에서 메시지를 보낼 수 없습니다.");
		}

		ChatMessage message = chatMessageRepository.save(new ChatMessage(chatRoom.getId(), member.getUserId(), content));
		chatRoom.updateLastMessageAt(message.getCreatedAt());
		User sender = userRepository.findById(member.getUserId()).orElse(null);
		ExchangeRequest request = exchangeRequestRepository.findById(chatRoom.getExchangeRequestId()).orElse(null);
		Group group = request == null ? null : request.getGroup();
		GroupMemberStatus groupMemberStatus = group == null ? null
				: groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), senderId)
						.map(groupMember -> groupMember.getStatus())
						.orElse(null);

		return chatMessageResponseMapper.toResponse(message, sender, groupMemberStatus, null, Map.of());
	}

	@Transactional
	public void markReadThrough(Long chatRoomId, Long userId, Long messageId) {
		ChatMember member = chatMemberRepository.findActiveMember(chatRoomId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
		member.markReadThrough(messageId);
	}

	@Transactional
	public void sendSystemMessage(ChatRoom chatRoom, ChatSystemEventType eventType) {
		ChatMessage message = chatMessageRepository.save(ChatMessage.system(chatRoom.getId(), eventType));
		chatRoom.updateLastMessageAt(message.getCreatedAt());
		ExchangeRequest request = ChatMessageResponseMapper.isExchangeRequestEvent(eventType)
				? exchangeRequestRepository.findById(chatRoom.getExchangeRequestId()).orElse(null)
				: null;
		Map<Long, String> thumbnails = chatMessageResponseMapper.findSystemMessageThumbnails(List.of(message), request);
		ChatMessageResponse response = chatMessageResponseMapper.toResponse(message, null, null, request, thumbnails);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					chatMessagePublisher.publish(response);
				} catch (RuntimeException exception) {
					if (log.isWarnEnabled()) {
						log.warn("Could not broadcast chat system message for room {}", chatRoom.getId(), exception);
					}
				}
			}
		});
	}
}

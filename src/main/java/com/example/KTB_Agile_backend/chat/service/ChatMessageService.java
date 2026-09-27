package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessagePageResponse;
import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatRoomStatus;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private static final int MAX_CONTENT_LENGTH = 2000;
	private static final int PAGE_SIZE = 20;
	private static final int FETCH_SIZE = PAGE_SIZE + 1;

	private final ChatMemberRepository chatMemberRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Transactional
	public ChatMessageResponse send(Long chatRoomId, Long senderId, String content) {
		if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("메시지는 1~2000자여야 합니다.");
		}

		ChatMember member = chatMemberRepository
				.findActiveMember(chatRoomId, senderId)
				.orElseThrow(() -> new AccessDeniedException("채팅방 멤버만 메시지를 보낼 수 있습니다."));
		if (member.getChatRoom().isDeleted()
				|| member.getChatRoom().getChatRoomStatus() != ChatRoomStatus.OPEN) {
			throw new AccessDeniedException("현재 채팅방에서 메시지를 보낼 수 없습니다.");
		}

		ChatMessage message = chatMessageRepository.saveAndFlush(
				new ChatMessage(member.getChatRoom(), member.getUser(), content));
		member.getChatRoom().updateLastMessageAt(message.getCreatedAt());

		return toResponse(message);
	}

	@Transactional(readOnly = true)
	public ChatMessagePageResponse findMessages(Long chatRoomId, Long userId, String cursor) {
		if (chatMemberRepository.findActiveMember(chatRoomId, userId).isEmpty()) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}

		Long cursorId = CursorCodec.decodeId(cursor);
		Pageable pageable = PageRequest.of(0, FETCH_SIZE);
		List<ChatMessage> fetchedMessages = cursorId == null
				? chatMessageRepository.findAllByChatRoom_IdOrderByIdDesc(chatRoomId, pageable)
				: chatMessageRepository.findAllByChatRoom_IdAndIdLessThanOrderByIdDesc(
						chatRoomId, cursorId, pageable);

		boolean hasNext = fetchedMessages.size() > PAGE_SIZE;
		List<ChatMessage> messages = new ArrayList<>(hasNext
				? fetchedMessages.subList(0, PAGE_SIZE)
				: fetchedMessages);
		String nextCursor = hasNext
				? CursorCodec.encodeId(messages.get(messages.size() - 1).getId())
				: null;
		Collections.reverse(messages);

		return new ChatMessagePageResponse(
				messages.stream().map(ChatMessageService::toResponse).toList(), nextCursor, hasNext
		);
	}

	private static ChatMessageResponse toResponse(ChatMessage message) {
		return new ChatMessageResponse(
				message.getId(), message.getChatRoom().getId(), message.getUser().getId(),
				message.getContent(), message.getMessageType(), message.getCreatedAt()
		);
	}
}

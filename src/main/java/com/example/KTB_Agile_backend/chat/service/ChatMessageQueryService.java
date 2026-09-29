package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessagePageResponse;
import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageQueryService {

	private static final int PAGE_SIZE = 20;
	private static final int FETCH_SIZE = PAGE_SIZE + 1;

	private final ChatMemberRepository chatMemberRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Transactional(readOnly = true)
	public ChatMessagePageResponse findMessages(Long chatRoomId, Long userId, String cursor) {
		chatMemberRepository.findActiveMember(chatRoomId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));

		Long cursorId = CursorCodec.decodeId(cursor);
		Pageable pageable = PageRequest.of(0, FETCH_SIZE);
		List<ChatMessage> fetchedMessages = cursorId == null
				? chatMessageRepository.findAllByChatRoom_IdOrderByIdDesc(chatRoomId, pageable)
				: chatMessageRepository.findAllByChatRoom_IdAndIdLessThanOrderByIdDesc(
						chatRoomId, cursorId, pageable);

		CursorPage<ChatMessage> page = CursorPage.fromIds(fetchedMessages, PAGE_SIZE, ChatMessage::getId);
		List<ChatMessage> messages = new ArrayList<>(page.items());
		Collections.reverse(messages);

		return new ChatMessagePageResponse(
				messages.stream().map(ChatMessageQueryService::toResponse).toList(),
				page.nextCursor(),
				page.hasNext()
		);
	}

	private static ChatMessageResponse toResponse(ChatMessage message) {
		return new ChatMessageResponse(
				message.getId(), message.getChatRoom().getId(), message.getUser().getId(),
				message.getContent(), message.getMessageType(), message.getCreatedAt()
		);
	}
}

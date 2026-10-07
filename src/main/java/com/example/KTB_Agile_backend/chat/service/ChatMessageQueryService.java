package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessagePageResponse;
import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatMessageQueryService {

	private static final int PAGE_SIZE = 20;
	private static final int FETCH_SIZE = PAGE_SIZE + 1;

	private final ChatMemberRepository chatMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final GroupMemberRepository groupMemberRepository;

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
		Map<Long, GroupMemberStatus> groupMemberStatuses = findGroupMemberStatuses(messages);

		return new ChatMessagePageResponse(
				messages.stream()
						.map(message -> toResponse(message, groupMemberStatuses.get(message.getUser().getId())))
						.toList(),
				page.nextCursor(),
				page.hasNext()
		);
	}

	private Map<Long, GroupMemberStatus> findGroupMemberStatuses(List<ChatMessage> messages) {
		if (messages.isEmpty()) {
			return Map.of();
		}
		Group group = messages.get(0).getChatRoom().getExchangeRequest().getGroup();
		if (group == null) {
			return Map.of();
		}
		List<Long> userIds = messages.stream().map(message -> message.getUser().getId()).distinct().toList();
		Map<Long, GroupMemberStatus> statuses = new HashMap<>();
		groupMemberRepository.findStatusesByGroupIdsAndUserIds(List.of(group.getId()), userIds)
				.forEach(status -> statuses.put(status.getUserId(), status.getMembershipStatus()));
		return statuses;
	}

	private static ChatMessageResponse toResponse(ChatMessage message, GroupMemberStatus groupMemberStatus) {
		return new ChatMessageResponse(
				message.getId(), message.getChatRoom().getId(), message.getUser().getId(),
				message.getContent(), message.getMessageType(), message.getCreatedAt(),
				message.getUser().getUserStatus(), groupMemberStatus
		);
	}
}

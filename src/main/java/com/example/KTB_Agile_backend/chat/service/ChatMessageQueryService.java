package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessagePageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatRoomRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
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
	private final ChatRoomRepository chatRoomRepository;
	private final ExchangeRequestRepository exchangeRequestRepository;
	private final UserRepository userRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ChatMessageResponseMapper chatMessageResponseMapper;

	@Transactional(readOnly = true)
	public ChatMessagePageResponse findMessages(Long chatRoomId, Long userId, String cursor) {
		chatMemberRepository.findActiveMember(chatRoomId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));

		Long cursorId = CursorCodec.decodeId(cursor);
		Pageable pageable = PageRequest.of(0, FETCH_SIZE);
		List<ChatMessage> fetchedMessages = cursorId == null
				? chatMessageRepository.findAllByChatRoomIdOrderByIdDesc(chatRoomId, pageable)
				: chatMessageRepository.findAllByChatRoomIdAndIdLessThanOrderByIdDesc(
						chatRoomId, cursorId, pageable);

		CursorPage<ChatMessage> page = CursorPage.fromIds(fetchedMessages, PAGE_SIZE, ChatMessage::getId);
		List<ChatMessage> messages = new ArrayList<>(page.items());
		Collections.reverse(messages);
		ExchangeRequest request = chatRoomRepository.findById(chatRoomId)
				.flatMap(room -> exchangeRequestRepository.findById(room.getExchangeRequestId()))
				.orElse(null);
		Map<Long, User> users = new HashMap<>();
		userRepository.findAllById(messages.stream()
				.map(ChatMessage::getUserId)
				.filter(userIdValue -> userIdValue != null)
				.distinct()
				.toList())
				.forEach(user -> users.put(user.getId(), user));
		Map<Long, GroupMemberStatus> groupMemberStatuses = findGroupMemberStatuses(messages, request);
		Map<Long, String> thumbnails = chatMessageResponseMapper.findSystemMessageThumbnails(messages, request);

		return new ChatMessagePageResponse(
				messages.stream()
						.map(message -> chatMessageResponseMapper.toResponse(
								message,
								users.get(message.getUserId()),
								message.getUserId() == null ? null : groupMemberStatuses.get(message.getUserId()),
								request,
								thumbnails))
						.toList(),
				page.nextCursor(),
				page.hasNext()
		);
	}

	private Map<Long, GroupMemberStatus> findGroupMemberStatuses(List<ChatMessage> messages, ExchangeRequest request) {
		if (messages.isEmpty() || request == null || request.getGroup() == null) {
			return Map.of();
		}
		List<Long> userIds = messages.stream()
				.map(ChatMessage::getUserId)
				.filter(userId -> userId != null)
				.distinct()
				.toList();
		if (userIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, GroupMemberStatus> statuses = new HashMap<>();
		groupMemberRepository.findStatusesByGroupIdsAndUserIds(List.of(request.getGroup().getId()), userIds)
				.forEach(status -> statuses.put(status.getUserId(), status.getMembershipStatus()));
		return statuses;
	}
}

package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessagePageResponse;
import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatSystemEventType;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.OfferedItem;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.image.repository.ImageRepository;
import com.example.KTB_Agile_backend.image.service.ImageUrlResolver;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatMessageQueryService {

	private static final int PAGE_SIZE = 20;
	private static final int FETCH_SIZE = PAGE_SIZE + 1;

	private final ChatMemberRepository chatMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ImageRepository imageRepository;
	private final ImageUrlResolver imageUrlResolver;

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
		Map<Long, String> thumbnails = findSystemMessageThumbnails(messages);

		return new ChatMessagePageResponse(
				messages.stream()
						.map(message -> {
							User user = message.getUser();
							return toResponse(message,
									user == null ? null : groupMemberStatuses.get(user.getId()), thumbnails);
						})
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
		List<Long> userIds = messages.stream()
				.map(ChatMessage::getUser)
				.filter(user -> user != null)
				.map(User::getId)
				.distinct().toList();
		if (userIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, GroupMemberStatus> statuses = new HashMap<>();
		groupMemberRepository.findStatusesByGroupIdsAndUserIds(List.of(group.getId()), userIds)
				.forEach(status -> statuses.put(status.getUserId(), status.getMembershipStatus()));
		return statuses;
	}

	private Map<Long, String> findSystemMessageThumbnails(List<ChatMessage> messages) {
		Set<Long> itemIds = new HashSet<>();
		for (ChatMessage message : messages) {
			if (!hasExchangeRequestPayload(message)) {
				continue;
			}
			ExchangeRequest request = message.getChatRoom().getExchangeRequest();
			itemIds.add(request.getItem().getId());
			request.getOfferedItems().stream().limit(2)
					.map(offered -> offered.getItem().getId())
					.forEach(itemIds::add);
		}
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> thumbnails = new HashMap<>();
		for (Image image : imageRepository.findThumbnailImagesByItemIds(itemIds)) {
			thumbnails.put(image.getItem().getId(), imageUrlResolver.resolve(image));
		}
		return thumbnails;
	}

	public ChatMessageResponse toSystemMessageResponse(ChatMessage message) {
		return toResponse(message, null, findSystemMessageThumbnails(List.of(message)));
	}

	private static ChatMessageResponse toResponse(
			ChatMessage message,
			GroupMemberStatus groupMemberStatus,
			Map<Long, String> thumbnails
	) {
		User user = message.getUser();
		return new ChatMessageResponse(
				message.getId(), message.getChatRoom().getId(), user == null ? null : user.getId(),
				message.getContent(), message.getMessageType(), message.getCreatedAt(),
				user == null ? null : user.getUserStatus(), groupMemberStatus,
				hasExchangeRequestPayload(message)
						? toPayload(message, thumbnails) : null
		);
	}

	private static boolean hasExchangeRequestPayload(ChatMessage message) {
		return message.getSystemEventType() == ChatSystemEventType.EXCHANGE_REQUEST_CREATED
				|| message.getSystemEventType() == ChatSystemEventType.EXCHANGE_REQUEST_UPDATED;
	}

	private static ChatMessageResponse.Payload toPayload(ChatMessage message, Map<Long, String> thumbnails) {
		ExchangeRequest request = message.getChatRoom().getExchangeRequest();
		Item requestedItem = request.getItem();
		List<OfferedItem> offeredItems = request.getOfferedItems();
		return new ChatMessageResponse.Payload(
				"EXCHANGE_REQUEST_CREATED",
				request.getId(),
				new ChatMessageResponse.RequestedItem(
						requestedItem.getId(), requestedItem.getTitle(), requestedItem.getQuantity(),
						thumbnails.get(requestedItem.getId())),
				request.getRequestedQuantity(),
				java.util.stream.IntStream.range(0, offeredItems.size())
						.mapToObj(index -> {
							OfferedItem offered = offeredItems.get(index);
							Item item = offered.getItem();
							return new ChatMessageResponse.OfferedItem(
									item.getId(), item.getTitle(), offered.getQuantity(), item.getQuantity(),
									index < 2 ? thumbnails.get(item.getId()) : null);
						})
						.toList()
		);
	}
}

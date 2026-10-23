package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatRoomDetailResponse;
import com.example.KTB_Agile_backend.chat.dto.response.ChatRoomPageResponse;
import com.example.KTB_Agile_backend.chat.dto.response.ChatRoomSummary;
import com.example.KTB_Agile_backend.chat.entity.ChatMember;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatRoom;
import com.example.KTB_Agile_backend.chat.repository.ChatMemberRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatMessageRepository;
import com.example.KTB_Agile_backend.chat.repository.ChatRoomRepository;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.OfferedItem;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomQueryService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final String INVALID_PAGE_MESSAGE = "direction, size 또는 cursor 값이 올바르지 않습니다.";
	private final ChatMemberRepository chatMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ExchangeRequestRepository exchangeRequestRepository;
	private final ImageRepository imageRepository;
	private final ImageUrlResolver imageUrlResolver;

	@Transactional(readOnly = true)
	public ChatRoomPageResponse findChatRooms(Long userId, String directionValue, String sizeValue, String cursorValue) {
		String direction = parseDirection(directionValue);
		int size = parseSize(sizeValue);
		CursorCodec.TimeIdCursor cursor = CursorCodec.decodeTimeId(cursorValue);
		Pageable pageable = PageRequest.of(0, size + 1);
		List<ChatMember> fetched = cursor == null
				? chatMemberRepository.findChatRoomsByUserId(userId, direction, pageable)
				: chatMemberRepository.findChatRoomsByUserIdAfter(
						userId, direction, cursor.time(), cursor.id(), pageable);
		Map<Long, ChatRoom> rooms = findRooms(fetched);

		CursorPage<ChatMember> page = CursorPage.from(
				fetched, size, member -> CursorCodec.encodeTimeId(
						rooms.get(member.getChatRoomId()).getLastMessageAt(), member.getChatRoomId()));
		if (page.items().isEmpty()) {
			return new ChatRoomPageResponse(List.of(), null, false);
		}

		List<Long> roomIds = page.items().stream().map(ChatMember::getChatRoomId).toList();
		List<ChatRoom> pageRooms = page.items().stream().map(member -> rooms.get(member.getChatRoomId())).toList();
		Map<Long, ExchangeRequest> exchangeRequests = new HashMap<>();
		List<Long> itemIds = new ArrayList<>();
		exchangeRequestRepository.findChatDetailsByIdIn(
				pageRooms.stream().map(ChatRoom::getExchangeRequestId).toList())
				.forEach(request -> {
					exchangeRequests.put(request.getId(), request);
					itemIds.add(request.getItem().getId());
					itemIds.add(request.getOfferedItems().get(0).getItem().getId());
				});
		Map<Long, ChatMessage> latestMessages = new HashMap<>();
		chatMessageRepository.findLatestByChatRoomIds(roomIds)
				.forEach(message -> latestMessages.put(message.getChatRoomId(), message));
		Map<Long, Long> unreadCounts = new HashMap<>();
		chatMemberRepository.findUnreadMessageCounts(userId, roomIds)
				.forEach(count -> unreadCounts.put(count.getChatRoomId(), count.getUnreadMessageCount()));
		Map<Long, String> thumbnails = findThumbnails(itemIds);
		Map<GroupMembershipKey, GroupMemberStatus> groupMemberStatuses = findOtherUserGroupStatuses(
				userId, page.items(), rooms, exchangeRequests);

		List<ChatRoomSummary> chatRooms = page.items().stream()
				.map(member -> {
					ChatRoom room = rooms.get(member.getChatRoomId());
					ExchangeRequest request = exchangeRequests.get(room.getExchangeRequestId());
					return toSummary(userId, room, request, latestMessages, unreadCounts, thumbnails,
							groupMemberStatuses);
				})
				.toList();
		return new ChatRoomPageResponse(chatRooms, page.nextCursor(), page.hasNext());
	}

	@Transactional(readOnly = true)
	public ChatRoomDetailResponse findChatRoom(Long chatRoomId, Long userId) {
		ChatMember member = chatMemberRepository.findActiveMember(chatRoomId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
		ChatRoom room = chatRoomRepository.findById(member.getChatRoomId())
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
		ExchangeRequest request = exchangeRequestRepository.findChatDetailsByIdIn(List.of(room.getExchangeRequestId()))
				.stream().findFirst().orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
		Item requestedItem = request.getItem();
		List<OfferedItem> offeredItems = request.getOfferedItems();
		List<Long> itemIds = new ArrayList<>();
		itemIds.add(requestedItem.getId());
		offeredItems.forEach(offered -> itemIds.add(offered.getItem().getId()));
		Map<Long, String> thumbnails = findThumbnails(itemIds);
		String direction = request.getRequester().getId().equals(userId) ? "SENT" : "RECEIVED";

		return new ChatRoomDetailResponse(
				room.getId(), room.getChatRoomStatus(), direction,
				new ChatRoomDetailResponse.ExchangeRequestInfo(
						request.getId(), request.getRequestedStatus(),
						new ChatRoomDetailResponse.RequestedItem(
								requestedItem.getId(), requestedItem.getTitle(), requestedItem.getQuantity(),
								thumbnails.get(requestedItem.getId())),
						request.getRequestedQuantity(),
						java.util.stream.IntStream.range(0, offeredItems.size())
								.mapToObj(index -> {
									OfferedItem offered = offeredItems.get(index);
									Item item = offered.getItem();
									return new ChatRoomDetailResponse.OfferedItem(
											item.getId(), item.getTitle(), offered.getQuantity(), item.getQuantity(),
											index < 2 ? thumbnails.get(item.getId()) : null);
								})
								.toList()
				)
		);
	}

	private Map<GroupMembershipKey, GroupMemberStatus> findOtherUserGroupStatuses(
			Long userId,
			List<ChatMember> members,
			Map<Long, ChatRoom> rooms,
			Map<Long, ExchangeRequest> exchangeRequests
	) {
		Set<Long> groupIds = new HashSet<>();
		Set<Long> otherUserIds = new HashSet<>();
		for (ChatMember member : members) {
			ChatRoom room = rooms.get(member.getChatRoomId());
			ExchangeRequest request = exchangeRequests.get(room.getExchangeRequestId());
			if (request.getGroup() == null) {
				continue;
			}
			groupIds.add(request.getGroup().getId());
			boolean sent = request.getRequester().getId().equals(userId);
			User otherUser = sent ? request.getItem().getUser() : request.getRequester();
			otherUserIds.add(otherUser.getId());
		}
		if (groupIds.isEmpty()) {
			return Map.of();
		}

		Map<GroupMembershipKey, GroupMemberStatus> statuses = new HashMap<>();
		groupMemberRepository.findStatusesByGroupIdsAndUserIds(groupIds, otherUserIds)
				.forEach(status -> statuses.put(
						new GroupMembershipKey(status.getGroupId(), status.getUserId()),
						status.getMembershipStatus()));
		return statuses;
	}

	private Map<Long, String> findThumbnails(List<Long> itemIds) {
		Map<Long, String> thumbnails = new HashMap<>();
		for (Image image : imageRepository.findThumbnailImagesByItemIds(itemIds)) {
			thumbnails.putIfAbsent(image.getItem().getId(), imageUrlResolver.resolve(image));
		}
		return thumbnails;
	}

	private static ChatRoomSummary toSummary(
			Long userId,
			ChatRoom room,
			ExchangeRequest request,
			Map<Long, ChatMessage> latestMessages,
			Map<Long, Long> unreadCounts,
			Map<Long, String> thumbnails,
			Map<GroupMembershipKey, GroupMemberStatus> groupMemberStatuses
	) {
		Item item = request.getItem();
		boolean sent = request.getRequester().getId().equals(userId);
		User otherUser = sent ? item.getUser() : request.getRequester();
		GroupMemberStatus groupMemberStatus = request.getGroup() == null ? null
				: groupMemberStatuses.get(new GroupMembershipKey(request.getGroup().getId(), otherUser.getId()));
		ChatMessage lastMessage = latestMessages.get(room.getId());
		ChatRoomSummary.LastMessage lastMessageResponse = lastMessage == null ? null
				: new ChatRoomSummary.LastMessage(
						lastMessage.getId(), lastMessage.getContent(),
						lastMessage.getUserId(),
						lastMessage.getCreatedAt());
		Item offeredItem = request.getOfferedItems().get(0).getItem();
		return new ChatRoomSummary(
				room.getId(), room.getChatRoomStatus(), request.getId(), request.getRequestedStatus(),
				sent ? "SENT" : "RECEIVED",
				request.getGroup() == null ? null : new ChatRoomSummary.GroupInfo(
						request.getGroup().getId(), request.getGroup().getGroupName()),
				new ChatRoomSummary.OtherUser(
						otherUser.getId(), otherUser.getNickname(), otherUser.getUserStatus(), groupMemberStatus),
				new ChatRoomSummary.TargetItem(item.getId(), item.getTitle(), thumbnails.get(item.getId())),
				new ChatRoomSummary.OfferedItem(
						offeredItem.getId(), offeredItem.getTitle(), thumbnails.get(offeredItem.getId())),
				lastMessageResponse, unreadCounts.getOrDefault(room.getId(), 0L),
				room.getLastMessageAt()
		);
	}

	private Map<Long, ChatRoom> findRooms(List<ChatMember> members) {
		Map<Long, ChatRoom> rooms = new HashMap<>();
		if (members.isEmpty()) {
			return rooms;
		}
		chatRoomRepository.findAllById(members.stream().map(ChatMember::getChatRoomId).distinct().toList())
				.forEach(room -> rooms.put(room.getId(), room));
		return rooms;
	}

	private static int parseSize(String sizeValue) {
		try {
			int size = Integer.parseInt(sizeValue);
			if (size >= 1 && size <= MAX_PAGE_SIZE) {
				return size;
			}
		} catch (NumberFormatException ignored) {
			// Return the endpoint-specific bad request below.
		}
		throw badPageRequest();
	}

	private static String parseDirection(String directionValue) {
		if (directionValue == null) {
			return null;
		}
		if ("SENT".equals(directionValue) || "RECEIVED".equals(directionValue)) {
			return directionValue;
		}
		throw badPageRequest();
	}

	private static ApiException badPageRequest() {
		return new ApiException(ErrorCode.BAD_REQUEST, INVALID_PAGE_MESSAGE);
	}

	private record GroupMembershipKey(Long groupId, Long userId) {
	}
}

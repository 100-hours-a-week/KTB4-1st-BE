package com.example.KTB_Agile_backend.chat.service;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;
import com.example.KTB_Agile_backend.chat.entity.ChatMessage;
import com.example.KTB_Agile_backend.chat.entity.ChatMessageType;
import com.example.KTB_Agile_backend.chat.entity.ChatSystemEventType;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.OfferedItem;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.image.repository.ImageRepository;
import com.example.KTB_Agile_backend.image.service.ImageUrlResolver;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class ChatMessageResponseMapper {

	private final ImageRepository imageRepository;
	private final ImageUrlResolver imageUrlResolver;

	public ChatMessageResponse toResponse(
			ChatMessage message,
			User user,
			GroupMemberStatus groupMemberStatus,
			ExchangeRequest request,
			Map<Long, String> thumbnails
	) {
		ChatSystemEventType eventType = message.getSystemEventType();
		return new ChatMessageResponse(
				message.getId(), message.getChatRoomId(), message.getUserId(),
				message.getContent(), message.getMessageType(), eventType, message.getCreatedAt(),
				user == null ? null : user.getUserStatus(), groupMemberStatus,
				isExchangeRequestEvent(eventType) && request != null ? toPayload(request, eventType, thumbnails) : null
		);
	}

	public Map<Long, String> findSystemMessageThumbnails(
			List<ChatMessage> messages,
			ExchangeRequest request
	) {
		if (request == null || messages.stream().noneMatch(message ->
				message.getMessageType() == ChatMessageType.SYSTEM
						&& isExchangeRequestEvent(message.getSystemEventType()))) {
			return Map.of();
		}
		Set<Long> itemIds = new HashSet<>();
		itemIds.add(request.getItem().getId());
		request.getOfferedItems().stream().limit(2)
				.map(offered -> offered.getItem().getId())
				.forEach(itemIds::add);
		Map<Long, String> thumbnails = new HashMap<>();
		for (Image image : imageRepository.findThumbnailImagesByItemIds(itemIds)) {
			thumbnails.put(image.getItem().getId(), imageUrlResolver.resolve(image));
		}
		return thumbnails;
	}

	static boolean isExchangeRequestEvent(ChatSystemEventType eventType) {
		return eventType == ChatSystemEventType.EXCHANGE_REQUEST_CREATED
				|| eventType == ChatSystemEventType.EXCHANGE_REQUEST_UPDATED;
	}

	private static ChatMessageResponse.Payload toPayload(
			ExchangeRequest request,
			ChatSystemEventType eventType,
			Map<Long, String> thumbnails
	) {
		Item requestedItem = request.getItem();
		List<OfferedItem> offeredItems = request.getOfferedItems();
		return new ChatMessageResponse.Payload(
				eventType.name(),
				request.getId(),
				new ChatMessageResponse.RequestedItem(
						requestedItem.getId(), requestedItem.getTitle(), requestedItem.getQuantity(),
						thumbnails.get(requestedItem.getId())),
				request.getRequestedQuantity(),
				IntStream.range(0, offeredItems.size())
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

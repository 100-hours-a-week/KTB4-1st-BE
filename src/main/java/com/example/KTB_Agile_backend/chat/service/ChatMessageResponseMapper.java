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

	public ChatMessageResponse toResponse(ChatMessage message, GroupMemberStatus groupMemberStatus) {
		return toResponse(message, groupMemberStatus, findSystemMessageThumbnails(List.of(message)));
	}

	public ChatMessageResponse toResponse(
			ChatMessage message,
			GroupMemberStatus groupMemberStatus,
			Map<Long, String> thumbnails
	) {
		User user = message.getUser();
		ChatSystemEventType eventType = message.getSystemEventType();
		return new ChatMessageResponse(
				message.getId(), message.getChatRoom().getId(), user == null ? null : user.getId(),
				message.getContent(), message.getMessageType(), eventType, message.getCreatedAt(),
				user == null ? null : user.getUserStatus(), groupMemberStatus,
				isExchangeRequestEvent(eventType) ? toPayload(message, thumbnails) : null
		);
	}

	public Map<Long, String> findSystemMessageThumbnails(List<ChatMessage> messages) {
		Set<Long> itemIds = new HashSet<>();
		for (ChatMessage message : messages) {
			if (message.getMessageType() != ChatMessageType.SYSTEM
					|| !isExchangeRequestEvent(message.getSystemEventType())) {
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

	private static boolean isExchangeRequestEvent(ChatSystemEventType eventType) {
		return eventType == ChatSystemEventType.EXCHANGE_REQUEST_CREATED
				|| eventType == ChatSystemEventType.EXCHANGE_REQUEST_UPDATED;
	}

	private static ChatMessageResponse.Payload toPayload(ChatMessage message, Map<Long, String> thumbnails) {
		ExchangeRequest request = message.getChatRoom().getExchangeRequest();
		Item requestedItem = request.getItem();
		List<OfferedItem> offeredItems = request.getOfferedItems();
		return new ChatMessageResponse.Payload(
				message.getSystemEventType().name(),
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

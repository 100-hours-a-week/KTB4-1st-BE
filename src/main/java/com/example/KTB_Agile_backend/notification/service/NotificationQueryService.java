package com.example.KTB_Agile_backend.notification.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.image.repository.ImageRepository;
import com.example.KTB_Agile_backend.image.service.ImageUrlResolver;
import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;
import com.example.KTB_Agile_backend.notification.dto.NotificationPageResponse;
import com.example.KTB_Agile_backend.notification.entity.Notification;
import com.example.KTB_Agile_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final NotificationRepository notificationRepository;
	private final ImageRepository imageRepository;
	private final ImageUrlResolver imageUrlResolver;

	@Transactional(readOnly = true)
	public NotificationPageResponse findMine(Long userId, String sizeValue, String cursorValue) {
		int size = parsePageSize(sizeValue);
		CursorCodec.TimeIdCursor cursor = CursorCodec.decodeTimeId(cursorValue);
		List<Notification> fetched = cursor == null
				? notificationRepository.findRecentByRecipient(userId, PageRequest.of(0, size + 1))
				: notificationRepository.findRecentByRecipientAfter(
						userId, cursor.time(), cursor.id(), PageRequest.of(0, size + 1));
		CursorPage<Notification> page = CursorPage.from(
				fetched,
				size,
				notification -> CursorCodec.encodeTimeId(notification.getCreatedAt(), notification.getId())
		);
		Map<Long, String> thumbnailImageUrls = findThumbnailImageUrls(page.items());
		List<NotificationMessage> notifications = page.items().stream()
				.map(notification -> toMessage(notification, thumbnailImageUrls.get(notification.getItemId())))
				.toList();
		return new NotificationPageResponse(notifications, page.nextCursor(), page.hasNext());
	}

	private static int parsePageSize(String sizeValue) {
		try {
			int size = Integer.parseInt(sizeValue);
			if (size >= 1 && size <= MAX_PAGE_SIZE) {
				return size;
			}
		} catch (NumberFormatException ignored) {
		}
		throw new ApiException(ErrorCode.BAD_REQUEST, "size 값이 올바르지 않습니다.");
	}

	private Map<Long, String> findThumbnailImageUrls(List<Notification> notifications) {
		if (notifications.isEmpty()) {
			return Map.of();
		}
		List<Long> itemIds = notifications.stream().map(Notification::getItemId).distinct().toList();
		Map<Long, String> thumbnailImageUrls = new HashMap<>();
		for (Image image : imageRepository.findThumbnailImagesByItemIds(itemIds)) {
			thumbnailImageUrls.put(image.getItem().getId(), imageUrlResolver.resolveThumbnail(image));
		}
		return thumbnailImageUrls;
	}

	private static NotificationMessage toMessage(Notification notification, String thumbnailImageUrl) {
		return new NotificationMessage(
				notification.getId(), notification.getItemId(), notification.getCreatedAt(), notification.getText(),
				thumbnailImageUrl);
	}
}

package com.example.KTB_Agile_backend.notification.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;
import com.example.KTB_Agile_backend.notification.dto.NotificationPageResponse;
import com.example.KTB_Agile_backend.notification.entity.Notification;
import com.example.KTB_Agile_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final NotificationRepository notificationRepository;

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
		List<NotificationMessage> notifications = page.items().stream()
				.map(NotificationQueryService::toMessage)
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

	private static NotificationMessage toMessage(Notification notification) {
		return new NotificationMessage(notification.getId(), notification.getCreatedAt(), notification.getText());
	}
}

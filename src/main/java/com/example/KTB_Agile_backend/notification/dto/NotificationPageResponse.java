package com.example.KTB_Agile_backend.notification.dto;

import java.util.List;

public record NotificationPageResponse(
		List<NotificationMessage> notifications,
		String nextCursor,
		boolean hasNext
) {

	public NotificationPageResponse {
		notifications = List.copyOf(notifications);
	}
}

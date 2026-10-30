package com.example.KTB_Agile_backend.notification.messaging;

import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;

@FunctionalInterface
public interface NotificationMessagePublisher {

	void publish(Long userId, NotificationMessage notification);
}

package com.example.KTB_Agile_backend.notification.messaging;

import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;

public interface NotificationMessagePublisher {

	void publish(Long userId, NotificationMessage notification);
}

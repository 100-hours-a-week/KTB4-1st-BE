package com.example.KTB_Agile_backend.notification.messaging;

import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;

public record RedisNotificationEvent(Long userId, NotificationMessage notification) {
}

package com.example.KTB_Agile_backend.notification.dto;

import java.time.LocalDateTime;

public record NotificationMessage(Long notificationId, LocalDateTime createdAt, String text) {
}

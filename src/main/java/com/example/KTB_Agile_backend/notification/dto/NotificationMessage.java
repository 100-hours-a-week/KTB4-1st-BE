package com.example.KTB_Agile_backend.notification.dto;

import java.util.Map;

public record NotificationMessage(String type, String title, String body, Map<String, Object> data) {
}

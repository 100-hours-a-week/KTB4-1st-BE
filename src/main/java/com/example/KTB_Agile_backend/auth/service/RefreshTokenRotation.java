package com.example.KTB_Agile_backend.auth.service;

import java.time.Duration;

public record RefreshTokenRotation(String refreshToken, Long userId, Duration remainingTtl) {
}

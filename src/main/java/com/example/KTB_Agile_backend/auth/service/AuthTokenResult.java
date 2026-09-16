package com.example.KTB_Agile_backend.auth.service;

import com.example.KTB_Agile_backend.auth.dto.response.AuthResponse;

public record AuthTokenResult(AuthResponse response, String refreshToken) {
}

package com.example.KTB_Agile_backend.auth.dto.response;

public record OAuthStateResponse(String state, Long expiresIn) {
}

package com.example.KTB_Agile_backend.ai.text.dto.response;

public record ModerationCheckResponse(
		boolean isAppropriate,
		String rejectionReason,
		String checkId
) {
}

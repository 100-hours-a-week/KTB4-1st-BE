package com.example.KTB_Agile_backend.item.moderation.dto.response;

public record ModerationCheckResponse(
		boolean isAppropriate,
		String rejectionReason,
		String checkId
) {
}

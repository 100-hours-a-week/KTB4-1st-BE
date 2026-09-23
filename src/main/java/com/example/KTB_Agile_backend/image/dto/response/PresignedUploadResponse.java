package com.example.KTB_Agile_backend.image.dto.response;

public record PresignedUploadResponse(
		String uploadUrl,
		String objectKey,
		long expiresInSeconds
) {
}

package com.example.KTB_Agile_backend.image.dto.response;

import java.util.Map;

public record PresignedUploadResponse(
		String uploadUrl,
		String objectKey,
		long expiresInSeconds,
		Map<String, String> requiredHeaders
) {

	public PresignedUploadResponse(String uploadUrl, String objectKey, long expiresInSeconds) {
		this(uploadUrl, objectKey, expiresInSeconds, Map.of());
	}
}

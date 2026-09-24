package com.example.KTB_Agile_backend.group.dto.response;

import java.time.LocalDateTime;

public record GroupSummary(
		Long groupId,
		String groupName,
		String roadAddress,
		String groupContent,
		Long memberCount,
		Long itemCount,
		LocalDateTime lastItemCreatedAt,
		Boolean isJoined
) {
}

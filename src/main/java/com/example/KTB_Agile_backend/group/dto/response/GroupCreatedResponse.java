package com.example.KTB_Agile_backend.group.dto.response;

import com.example.KTB_Agile_backend.group.entity.Group;

import java.time.LocalDateTime;

public record GroupCreatedResponse(
		Long groupId,
		LocalDateTime createdAt
) {

	public static GroupCreatedResponse from(Group group) {
		return new GroupCreatedResponse(group.getId(), group.getCreatedAt());
	}
}

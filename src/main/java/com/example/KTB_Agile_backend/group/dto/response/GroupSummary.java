package com.example.KTB_Agile_backend.group.dto.response;

import com.example.KTB_Agile_backend.group.entity.Group;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GroupSummary(
		Long groupId,
		String groupName,
		String roadAddress,
		BigDecimal longitude,
		BigDecimal latitude,
		String groupContent,
		LocalDateTime createdAt
) {

	public static GroupSummary from(Group group) {
		return new GroupSummary(
				group.getId(),
				group.getGroupName(),
				group.getRoadAddress(),
				group.getLongitude(),
				group.getLatitude(),
				group.getGroupContent(),
				group.getCreatedAt()
		);
	}
}

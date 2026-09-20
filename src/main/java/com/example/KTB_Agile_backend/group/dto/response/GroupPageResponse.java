package com.example.KTB_Agile_backend.group.dto.response;

import java.util.List;

public record GroupPageResponse(
		List<GroupSummary> groups,
		int size,
		boolean hasNext,
		String nextCursor
) {

	public GroupPageResponse {
		groups = List.copyOf(groups);
	}
}

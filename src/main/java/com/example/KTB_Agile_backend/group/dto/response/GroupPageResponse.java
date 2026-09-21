package com.example.KTB_Agile_backend.group.dto.response;

import java.util.List;

public record GroupPageResponse(
		List<GroupSummary> groups,
		String nextCursor,
		boolean hasNext
) {

	public GroupPageResponse {
		groups = List.copyOf(groups);
	}
}

package com.example.KTB_Agile_backend.item.dto.response;

import com.example.KTB_Agile_backend.item.entity.ItemState;

import java.time.LocalDateTime;
import java.util.List;

public record MyItemPageResponse(
		List<MyItem> items,
		String nextCursor,
		boolean hasNext
) {

	public MyItemPageResponse {
		items = List.copyOf(items);
	}

	public record MyItem(
			Long itemId,
			List<GroupInfo> groups,
			String title,
			String contentPreview,
			Integer quantity,
			ItemState itemState,
			String thumbnailImageUrl,
			Long likeCount,
			Long exchangeRequestCount,
			Boolean isLiked,
			LocalDateTime createdAt
	) {

		public MyItem {
			groups = List.copyOf(groups);
		}
	}

	public record GroupInfo(Long groupId, String groupName) {
	}
}

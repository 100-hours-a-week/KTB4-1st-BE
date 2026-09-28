package com.example.KTB_Agile_backend.chat.dto.response;

import java.util.List;

public record ChatRoomPageResponse(
		List<ChatRoomSummary> chatRooms,
		String nextCursor,
		boolean hasNext
) {

	public ChatRoomPageResponse {
		chatRooms = List.copyOf(chatRooms);
	}

}

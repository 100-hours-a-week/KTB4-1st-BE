package com.example.KTB_Agile_backend.chat.dto.response;

import java.util.List;

public record ChatMessagePageResponse(
		List<ChatMessageResponse> messages,
		String nextCursor,
		boolean hasNext
) {

	public ChatMessagePageResponse {
		messages = List.copyOf(messages);
	}
}

package com.example.KTB_Agile_backend.chat.messaging;

import com.example.KTB_Agile_backend.chat.dto.response.ChatMessageResponse;

@FunctionalInterface
public interface ChatMessagePublisher {

	void publish(ChatMessageResponse message);
}

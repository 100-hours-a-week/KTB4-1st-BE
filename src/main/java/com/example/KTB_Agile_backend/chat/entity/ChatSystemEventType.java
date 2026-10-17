package com.example.KTB_Agile_backend.chat.entity;

public enum ChatSystemEventType {
	EXCHANGE_REQUEST_CREATED("교환 요청이 접수되었습니다."),
	EXCHANGE_REQUEST_UPDATED("교환 요청이 수정되었습니다."),
	CHAT_ROOM_MEMBER_LEFT("상대방이 채팅방을 나갔습니다"),
	EXCHANGE_REQUEST_REJECTED("교환이 거절 되었습니다."),
	EXCHANGE_REQUEST_CANCELED("교환이 취소 되었습니다.");

	private final String message;

	ChatSystemEventType(String content) {
		this.message = content;
	}

	public String content() {
		return message;
	}
}

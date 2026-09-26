package com.example.KTB_Agile_backend.chat.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_id", nullable = false)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exchange_request_id", nullable = false, unique = true)
	private ExchangeRequest exchangeRequest;

	@Enumerated(EnumType.STRING)
	@Column(name = "chat_room_status", nullable = false, length = 20)
	private ChatRoomStatus chatRoomStatus = ChatRoomStatus.OPEN;

	@Column(name = "last_message_at", nullable = false)
	private LocalDateTime lastMessageAt = LocalDateTime.now();

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public ChatRoom(ExchangeRequest exchangeRequest) {
		this.exchangeRequest = exchangeRequest;
	}
}

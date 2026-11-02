package com.example.KTB_Agile_backend.notification.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "notifications",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notifications_recipient_event_request",
				columnNames = {"recipient_user_id", "event_identity", "exchange_request_id"}),
		indexes = @Index(
				name = "idx_notifications_recipient_created_id",
				columnList = "recipient_user_id, created_at, notification_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id", nullable = false)
	private Long id;

	@Column(name = "recipient_user_id", nullable = false)
	private Long recipientUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private NotificationEventType eventType;

	@Column(name = "event_identity", nullable = false, length = 36)
	private String eventIdentity;

	@Column(name = "exchange_request_id", nullable = false)
	private Long exchangeRequestId;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "notification_text", nullable = false, length = 500)
	private String text;

	public Notification(
			Long recipientUserId,
			NotificationEventType eventType,
			String eventIdentity,
			Long exchangeRequestId,
			Long itemId,
			String text
	) {
		this.recipientUserId = recipientUserId;
		this.eventType = eventType;
		this.eventIdentity = eventIdentity;
		this.exchangeRequestId = exchangeRequestId;
		this.itemId = itemId;
		this.text = text;
	}
}

package com.example.KTB_Agile_backend.item.price.persistence;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationOutboxEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Getter;

@Entity
@Table(name = "item_price_estimation_outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ItemPriceEstimationOutboxEvent extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "outbox_event_id", nullable = false)
	private Long id;

	@Column(name = "event_id", nullable = false, length = 36, unique = true)
	private String eventId;

	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	@Column(name = "original_request_event_id", nullable = false, length = 36)
	private String originalRequestEventId;

	@Column(name = "deduplication_key", nullable = false, length = 128, unique = true)
	private String deduplicationKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private ItemPriceEstimationOutboxEventType eventType;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String payload;

	public ItemPriceEstimationOutboxEvent(String eventId, String requestId, String originalRequestEventId,
			String deduplicationKey, ItemPriceEstimationOutboxEventType eventType, Long itemId, int attemptCount,
			String payload) {
		this.eventId = eventId;
		this.requestId = requestId;
		this.originalRequestEventId = originalRequestEventId;
		this.deduplicationKey = deduplicationKey;
		this.eventType = eventType;
		this.itemId = itemId;
		this.attemptCount = attemptCount;
		this.payload = payload;
	}
}

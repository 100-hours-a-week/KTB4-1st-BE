package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_polling_relay_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxPollingRelayState extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false, length = 36, unique = true)
	private String eventId;

	public OutboxPollingRelayState(String eventId) {
		this.eventId = eventId;
	}
}

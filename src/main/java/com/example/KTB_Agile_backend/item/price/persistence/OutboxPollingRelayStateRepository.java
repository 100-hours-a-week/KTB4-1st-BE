package com.example.KTB_Agile_backend.item.price.persistence;

import com.example.KTB_Agile_backend.item.price.persistence.OutboxPollingRelayState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxPollingRelayStateRepository extends JpaRepository<OutboxPollingRelayState, Long> {
	boolean existsByEventId(String eventId);
}

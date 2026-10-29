package com.example.KTB_Agile_backend.item.price.messaging;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.item.price.persistence.ItemPriceEstimationOutboxEventRepository;
import com.example.KTB_Agile_backend.item.price.persistence.OutboxPollingRelayStateRepository;
import com.example.KTB_Agile_backend.item.price.persistence.OutboxPollingRelayState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Conditional(KafkaAddressCondition.Configured.class)
public class ItemPriceEstimationOutboxPollingRelay {
	private static final int PAGE_SIZE = 100;

	private final ItemPriceEstimationOutboxEventRepository outboxRepository;
	private final OutboxPollingRelayStateRepository relayStateRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Scheduled(fixedDelayString = "${app.kafka.outbox.poll-delay:1000}")
	public void publishPending() {
		Long afterId = 0L;
		while (true) {
			var events = outboxRepository.findUnpublishedAfterId(afterId, PageRequest.of(0, PAGE_SIZE));
			if (events.isEmpty()) return;
			for (var event : events) {
				afterId = event.getId();
				if (relayStateRepository.existsByEventId(event.getEventId())) continue;
				try {
					kafkaTemplate.send(event.getEventType().topic(), event.getEventId(), event.getPayload()).get();
					relayStateRepository.save(new OutboxPollingRelayState(event.getEventId()));
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted while relaying item price estimation event", exception);
				} catch (Exception exception) {
					if (log.isErrorEnabled()) {
						log.error("Failed to relay Kafka event: eventId={}, topic={}",
								event.getEventId(), event.getEventType().topic(), exception);
					}
				}
			}
			if (events.size() < PAGE_SIZE) return;
		}
	}
}

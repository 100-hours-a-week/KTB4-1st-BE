package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.item.service.ItemRecommendationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaItemRecommendationEventListenerTest {

	@Test
	void sendsItemIdAndKafkaPositionToRecommendationService() throws Exception {
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		ItemRecommendationService service = mock(ItemRecommendationService.class);
		String payload = "{\"userId\":42,\"itemId\":100}";
		when(objectMapper.readValue(payload, ItemViewEvent.class)).thenReturn(new ItemViewEvent(42L, 100L));
		KafkaItemRecommendationEventListener listener = new KafkaItemRecommendationEventListener(
				objectMapper, service);
		ConsumerRecord<String, String> record = new ConsumerRecord<>(
				"item-view-events", 2, 15L, "100", payload);
		long beforeConsume = System.currentTimeMillis();

		listener.consume(record);

		ArgumentCaptor<Long> timestamp = ArgumentCaptor.forClass(Long.class);
		verify(service).recordView(eq(100L), timestamp.capture(), eq(2), eq(15L));
		assertThat(timestamp.getValue()).isBetween(beforeConsume, System.currentTimeMillis());
	}
}

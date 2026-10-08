package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
		prefix = "app.item-view",
		name = "publisher",
		havingValue = "executor",
		matchIfMissing = true
)
public class ExecutorItemViewEventPublisher implements ItemViewEventPublisher {

	private final ItemService itemService;

	@Override
	@Async("viewCountExecutor")
	public void publish(ItemViewEvent event) {
		itemService.recordView(event.userId(), event.itemId());
	}
}

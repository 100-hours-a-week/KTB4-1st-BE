package com.example.KTB_Agile_backend.item.event;

import com.example.KTB_Agile_backend.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsyncItemViewCountPublisher {

	private final ItemService itemService;

	@Async("viewCountExecutor")
	public void publish(ItemViewEvent event) {
		itemService.recordView(event.userId(), event.itemId());
	}
}

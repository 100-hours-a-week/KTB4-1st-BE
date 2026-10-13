package com.example.KTB_Agile_backend.search.event;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import com.example.KTB_Agile_backend.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Conditional(KafkaAddressCondition.Missing.class)
public class ExecutorSearchHistoryEventPublisher implements SearchHistoryEventPublisher {

	private final SearchHistoryService searchHistoryService;

	@Override
	@Async("viewCountExecutor")
	public void publish(SearchHistoryEvent event) {
		searchHistoryService.recordProductSearch(event.userId(), event.keyword());
	}
}

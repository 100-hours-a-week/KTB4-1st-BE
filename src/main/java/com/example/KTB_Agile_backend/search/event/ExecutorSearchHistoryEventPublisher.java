package com.example.KTB_Agile_backend.search.event;

import com.example.KTB_Agile_backend.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
		prefix = "app.search-history",
		name = "publisher",
		havingValue = "executor",
		matchIfMissing = true
)
public class ExecutorSearchHistoryEventPublisher implements SearchHistoryEventPublisher {

	private final SearchHistoryService searchHistoryService;

	@Override
	@Async("viewCountExecutor")
	public void publish(SearchHistoryEvent event) {
		searchHistoryService.recordProductSearch(event.userId(), event.keyword());
	}
}

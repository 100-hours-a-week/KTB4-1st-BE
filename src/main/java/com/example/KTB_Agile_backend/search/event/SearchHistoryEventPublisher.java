package com.example.KTB_Agile_backend.search.event;

@FunctionalInterface
public interface SearchHistoryEventPublisher {

	void publish(SearchHistoryEvent event);
}

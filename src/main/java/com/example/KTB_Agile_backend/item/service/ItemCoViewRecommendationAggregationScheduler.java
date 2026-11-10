package com.example.KTB_Agile_backend.item.service;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemCoViewRecommendationAggregationScheduler {

	private static final long AGGREGATION_INTERVAL_MILLIS = 10 * 60 * 1000L;

	private final ItemCoViewRecommendationService recommendationService;

	@Scheduled(fixedDelay = AGGREGATION_INTERVAL_MILLIS)
	@SchedulerLock(
			name = "itemCoViewRecommendationAggregation",
			lockAtLeastFor = "PT1M",
			lockAtMostFor = "PT30M"
	)
	public void refreshScores() {
		recommendationService.refreshScores();
	}
}

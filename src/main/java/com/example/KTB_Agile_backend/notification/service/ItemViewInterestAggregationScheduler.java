package com.example.KTB_Agile_backend.notification.service;

import com.example.KTB_Agile_backend.common.config.KafkaAddressCondition;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Conditional(KafkaAddressCondition.Configured.class)
public class ItemViewInterestAggregationScheduler {
	private static final long AGGREGATION_INTERVAL_MILLIS = 60_000L;

	private final ItemViewInterestNotificationService service;

	@Scheduled(fixedDelay = AGGREGATION_INTERVAL_MILLIS)
	@SchedulerLock(
			name = "itemViewInterestAggregation",
			lockAtLeastFor = "PT1M"
	)
	public void aggregateRecentViewsAndNotify() {
		service.aggregateRecentViewsAndNotify();
	}
}

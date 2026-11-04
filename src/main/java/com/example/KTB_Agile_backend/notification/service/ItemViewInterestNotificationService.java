package com.example.KTB_Agile_backend.notification.service;

import com.example.KTB_Agile_backend.notification.repository.ItemViewInterestRepository;
import com.example.KTB_Agile_backend.notification.repository.ItemViewInterestRepository.ItemViewer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ItemViewInterestNotificationService {
	private static final int THRESHOLD = 10;
	private static final int WINDOW_HOURS = 6;

	private final ItemViewInterestRepository repository;
	private final NotificationService notificationService;

	@Transactional
	public void recordView(Long userId, Long itemId, long eventTimeMillis, int partition, long offset) {
		LocalDateTime viewedAt = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(eventTimeMillis), ZoneId.systemDefault());
		repository.insertViewIfAbsent(userId, itemId, viewedAt, partition, offset);
	}

	@Transactional
	public void aggregateRecentViewsAndNotify() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime cutoff = now.minusHours(WINDOW_HOURS);
		repository.deleteViewsBefore(cutoff);
		repository.deleteActiveItemsBelowThreshold(cutoff, now, THRESHOLD);

		Long currentItemId = null;
		boolean newlyQualified = false;
		for (ItemViewer viewer : repository.findViewersOfQualifiedItems(cutoff, now, THRESHOLD)) {
			if (!Objects.equals(currentItemId, viewer.itemId())) {
				currentItemId = viewer.itemId();
				newlyQualified = repository.insertActiveItemIfAbsent(currentItemId) > 0;
			}
			if (newlyQualified && repository.insertNotifiedUserIfAbsent(viewer.itemId(), viewer.userId()) > 0) {
				notificationService.itemInterestIncreased(viewer.userId(), viewer.itemId());
			}
		}
	}
}

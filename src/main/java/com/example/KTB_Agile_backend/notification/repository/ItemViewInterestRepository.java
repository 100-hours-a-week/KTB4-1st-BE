package com.example.KTB_Agile_backend.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemViewInterestRepository {
	void insertViewIfAbsent(Long userId, Long itemId, LocalDateTime viewedAt, int partition, long offset);

	void deleteViewsBefore(LocalDateTime cutoff);

	void deleteActiveItemsBelowThreshold(LocalDateTime cutoff, LocalDateTime currentTime, int threshold);

	List<ItemViewer> findViewersOfQualifiedItems(LocalDateTime cutoff, LocalDateTime currentTime, int threshold);

	int insertActiveItemIfAbsent(Long itemId);

	int insertNotifiedUserIfAbsent(Long itemId, Long userId);

	record ItemViewer(Long itemId, Long userId) {
	}
}

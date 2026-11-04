package com.example.KTB_Agile_backend.notification.service;

import com.example.KTB_Agile_backend.notification.dto.NotificationMessage;
import com.example.KTB_Agile_backend.notification.messaging.NotificationMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemViewInterestNotificationService {
	private static final int THRESHOLD = 10;
	private final JdbcTemplate jdbcTemplate;
	private final NotificationMessagePublisher publisher;

	@Transactional
	public void recordView(Long userId, Long itemId, long eventTimeMillis, int partition, long offset) {
		LocalDateTime viewedAt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(eventTimeMillis), java.time.ZoneId.systemDefault());
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime cutoff = now.minusHours(6);
		jdbcTemplate.update("INSERT IGNORE INTO item_view_interest_events (item_id, user_id, viewed_at, topic_partition, topic_offset) VALUES (?, ?, ?, ?, ?)", itemId, userId, Timestamp.valueOf(viewedAt), partition, offset);
		jdbcTemplate.update("DELETE FROM item_view_interest_events WHERE item_id = ? AND viewed_at < ?", itemId, Timestamp.valueOf(cutoff));
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item_view_interest_events WHERE item_id = ? AND viewed_at >= ? AND viewed_at <= ?", Integer.class, itemId, Timestamp.valueOf(cutoff), Timestamp.valueOf(now));
		if (count == null || count < THRESHOLD) return;
		List<Long> viewers = jdbcTemplate.queryForList("SELECT DISTINCT user_id FROM item_view_interest_events WHERE item_id = ? AND viewed_at >= ? AND viewed_at <= ?", Long.class, itemId, Timestamp.valueOf(cutoff), Timestamp.valueOf(now));
		for (Long viewerId : viewers) {
			int inserted = jdbcTemplate.update("INSERT IGNORE INTO item_view_interest_notified_users (item_id, user_id) VALUES (?, ?)", itemId, viewerId);
			if (inserted == 0) continue;
			String text = "최근 6시간 동안 이 물품의 조회 이벤트가 10회 이상 발생했습니다.";
			jdbcTemplate.update("INSERT INTO notifications (recipient_user_id, event_type, event_identity, exchange_request_id, item_id, notification_text, created_at) VALUES (?, 'ITEM_VIEW_INTEREST', ?, NULL, ?, ?, ?)", viewerId, UUID.randomUUID().toString(), itemId, text, Timestamp.valueOf(LocalDateTime.now()));
			Long notificationId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
			LocalDateTime createdAt = LocalDateTime.now();
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					try { publisher.publish(viewerId, new NotificationMessage(notificationId, createdAt, text)); }
					catch (RuntimeException exception) { log.warn("Could not publish item view interest notification {}", notificationId, exception); }
				}
			});
		}
	}

	@Scheduled(fixedDelay = 3600000)
	@Transactional
	public void removeExpiredEvents() {
		jdbcTemplate.update("DELETE FROM item_view_interest_events WHERE viewed_at < ?", Timestamp.valueOf(LocalDateTime.now().minusHours(6)));
	}
}

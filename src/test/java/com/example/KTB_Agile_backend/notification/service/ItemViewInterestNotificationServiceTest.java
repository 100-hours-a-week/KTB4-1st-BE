package com.example.KTB_Agile_backend.notification.service;

import com.example.KTB_Agile_backend.notification.repository.JdbcItemViewInterestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ItemViewInterestNotificationServiceTest {

	private JdbcTemplate jdbcTemplate;
	private NotificationService notificationService;
	private ItemViewInterestNotificationService service;

	@BeforeEach
	void setUp() {
		String databaseName = "item_interest_" + UUID.randomUUID().toString().replace("-", "");
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("""
				CREATE TABLE item_view_interest_events (
					event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
					item_id BIGINT NOT NULL,
					user_id BIGINT NOT NULL,
					viewed_at TIMESTAMP NOT NULL,
					topic_partition INT NOT NULL,
					topic_offset BIGINT NOT NULL,
					UNIQUE (topic_partition, topic_offset)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE item_view_interest_notified_users (
					item_id BIGINT NOT NULL,
					user_id BIGINT NOT NULL,
					PRIMARY KEY (item_id, user_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE item_view_interest_active_items (
					item_id BIGINT NOT NULL PRIMARY KEY
				)
				""");
		notificationService = mock(NotificationService.class);
		service = new ItemViewInterestNotificationService(
				new JdbcItemViewInterestRepository(jdbcTemplate), notificationService);
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.execute("SHUTDOWN");
	}

	@Test
	void aggregatesInBatchCountsRepeatedViewsAndNotifiesEachViewerOnlyOnce() {
		long now = System.currentTimeMillis();
		for (int offset = 0; offset < 10; offset++) {
			Long userId = offset < 9 ? 1L : 2L;
			service.recordView(userId, 100L, now - 60_000L, 0, offset);
		}
		for (int offset = 0; offset < 9; offset++) {
			service.recordView(3L, 200L, now - 60_000L, 0, offset + 10);
		}
		for (int offset = 0; offset < 9; offset++) {
			service.recordView(4L, 400L, now - 60_000L, 0, offset + 40);
		}
		service.recordView(4L, 400L, now - 60_000L, 0, 40);
		service.recordView(4L, 300L, now - 7 * 60 * 60 * 1000L, 0, 30);
		verifyNoInteractions(notificationService);

		service.aggregateRecentViewsAndNotify();
		service.aggregateRecentViewsAndNotify();

		verify(notificationService).itemInterestIncreased(1L, 100L);
		verify(notificationService).itemInterestIncreased(2L, 100L);
		verifyNoMoreInteractions(notificationService);
		Long remainingEvents = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM item_view_interest_events", Long.class);
		assertThat(remainingEvents).isEqualTo(28L);
		Long notifiedUsers = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM item_view_interest_notified_users", Long.class);
		assertThat(notifiedUsers).isEqualTo(2L);
	}
}

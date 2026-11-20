package com.example.KTB_Agile_backend.item.price.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItemPriceEstimationInboxRepository {
	private final JdbcTemplate jdbcTemplate;

	public boolean exists(String eventId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM item_price_estimation_inbox_events WHERE event_id = ?",
				Integer.class, eventId);
		return count != null && count > 0;
	}

	public void markProcessed(String eventId) {
		jdbcTemplate.update("""
				INSERT IGNORE INTO item_price_estimation_inbox_events (event_id, processed_at)
				VALUES (?, CURRENT_TIMESTAMP(6))
				""", eventId);
	}
}

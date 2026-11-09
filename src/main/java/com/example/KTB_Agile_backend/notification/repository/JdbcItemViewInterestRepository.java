package com.example.KTB_Agile_backend.notification.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcItemViewInterestRepository implements ItemViewInterestRepository {
	private static final RowMapper<ItemViewer> ITEM_VIEWER_ROW_MAPPER = (resultSet, rowNumber) ->
			new ItemViewer(resultSet.getLong("item_id"), resultSet.getLong("user_id"));

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void insertViewIfAbsent(
			Long userId,
			Long itemId,
			LocalDateTime viewedAt,
			int partition,
			long offset
	) {
		jdbcTemplate.update("""
				INSERT IGNORE INTO item_view_interest_events
					(item_id, user_id, viewed_at, topic_partition, topic_offset)
				VALUES (?, ?, ?, ?, ?)
				""", itemId, userId, Timestamp.valueOf(viewedAt), partition, offset);
	}

	@Override
	public void deleteViewsBefore(LocalDateTime cutoff) {
		jdbcTemplate.update(
				"DELETE FROM item_view_interest_events WHERE viewed_at < ?",
				Timestamp.valueOf(cutoff));
	}

	@Override
	public void deleteActiveItemsBelowThreshold(
			LocalDateTime cutoff,
			LocalDateTime currentTime,
			int threshold
	) {
		List<Long> inactiveItemIds = jdbcTemplate.queryForList("""
				SELECT active_items.item_id
				FROM item_view_interest_active_items active_items
				LEFT JOIN item_view_interest_events recent_views
					ON recent_views.item_id = active_items.item_id
					AND recent_views.viewed_at >= ? AND recent_views.viewed_at <= ?
				GROUP BY active_items.item_id
				HAVING COUNT(recent_views.event_id) < ?
				""", Long.class,
				Timestamp.valueOf(cutoff), Timestamp.valueOf(currentTime), threshold);
		if (!inactiveItemIds.isEmpty()) {
			jdbcTemplate.batchUpdate("DELETE FROM item_view_interest_active_items WHERE item_id = ?",
					inactiveItemIds, inactiveItemIds.size(),
					(preparedStatement, itemId) -> preparedStatement.setLong(1, itemId));
		}
	}

	@Override
	public List<ItemViewer> findViewersOfQualifiedItems(
			LocalDateTime cutoff,
			LocalDateTime currentTime,
			int threshold
	) {
		return jdbcTemplate.query("""
				SELECT DISTINCT recent_views.item_id, recent_views.user_id
				FROM item_view_interest_events recent_views
				JOIN (
					SELECT item_id
					FROM item_view_interest_events
					WHERE viewed_at >= ? AND viewed_at <= ?
					GROUP BY item_id
					HAVING COUNT(*) >= ?
				) qualified_items ON qualified_items.item_id = recent_views.item_id
				WHERE recent_views.viewed_at >= ? AND recent_views.viewed_at <= ?
				ORDER BY recent_views.item_id, recent_views.user_id
				""", ITEM_VIEWER_ROW_MAPPER,
				Timestamp.valueOf(cutoff), Timestamp.valueOf(currentTime), threshold,
				Timestamp.valueOf(cutoff), Timestamp.valueOf(currentTime));
	}

	@Override
	public int insertActiveItemIfAbsent(Long itemId) {
		return jdbcTemplate.update("""
				INSERT IGNORE INTO item_view_interest_active_items (item_id)
				VALUES (?)
				""", itemId);
	}

	@Override
	public int insertNotifiedUserIfAbsent(Long itemId, Long userId) {
		return jdbcTemplate.update("""
				INSERT IGNORE INTO item_view_interest_notified_users (item_id, user_id)
				VALUES (?, ?)
				""", itemId, userId);
	}
}

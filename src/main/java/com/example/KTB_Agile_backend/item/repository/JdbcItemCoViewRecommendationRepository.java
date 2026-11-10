package com.example.KTB_Agile_backend.item.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcItemCoViewRecommendationRepository {

	private final JdbcTemplate jdbcTemplate;

	public void recordView(Long userId, Long itemId, LocalDateTime viewedAt) {
		jdbcTemplate.update("""
				INSERT INTO item_co_view_user_items (user_id, item_id, last_viewed_at)
				VALUES (?, ?, ?)
				ON DUPLICATE KEY UPDATE last_viewed_at = IF(
					last_viewed_at <= DATE_SUB(VALUES(last_viewed_at), INTERVAL 24 HOUR),
					GREATEST(last_viewed_at, VALUES(last_viewed_at)),
					last_viewed_at
				)
				""", userId, itemId, Timestamp.valueOf(viewedAt));
	}

	public List<Long> findTopRelatedItemIds(Long itemId, int limit) {
		return jdbcTemplate.queryForList("""
				SELECT related_item_id
				FROM item_co_view_scores
				WHERE item_id = ?
				ORDER BY co_viewer_count DESC, related_item_id
				LIMIT ?
				""", Long.class, itemId, limit);
	}

	public void deleteUserItemViewsBefore(LocalDateTime cutoff) {
		jdbcTemplate.update("DELETE FROM item_co_view_user_items WHERE last_viewed_at < ?",
				Timestamp.valueOf(cutoff));
	}

	public void deleteScores() {
		jdbcTemplate.update("DELETE FROM item_co_view_scores");
	}

	public void insertScoresSince(LocalDateTime cutoff, LocalDateTime calculatedAt) {
		// shortcut: pairs all recent user-item rows; cap per-user history if scheduled refresh gets slow.
		jdbcTemplate.update("""
				INSERT INTO item_co_view_scores
					(item_id, related_item_id, co_viewer_count, calculated_at)
				SELECT viewed_item.item_id,
					other_item.item_id,
					COUNT(DISTINCT viewed_item.user_id),
					?
				FROM item_co_view_user_items viewed_item
				JOIN item_co_view_user_items other_item
					ON other_item.user_id = viewed_item.user_id
					AND other_item.item_id <> viewed_item.item_id
				WHERE viewed_item.last_viewed_at >= ?
					AND other_item.last_viewed_at >= ?
				GROUP BY viewed_item.item_id, other_item.item_id
				""", Timestamp.valueOf(calculatedAt), Timestamp.valueOf(cutoff), Timestamp.valueOf(cutoff));
	}
}

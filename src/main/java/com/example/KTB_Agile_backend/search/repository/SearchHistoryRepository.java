package com.example.KTB_Agile_backend.search.repository;

import com.example.KTB_Agile_backend.search.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
	String USER_ID_PARAM = "userId";

	@Modifying
	@Query(value = """
			INSERT INTO search_histories (user_id, keyword, last_searched_at, created_at, updated_at)
			VALUES (:userId, :keyword, :searchedAt, :searchedAt, :searchedAt)
			ON DUPLICATE KEY UPDATE
				last_searched_at = :searchedAt,
				updated_at = :searchedAt
			""", nativeQuery = true)
	void upsertActiveKeyword(
			@Param(USER_ID_PARAM) Long userId,
			@Param("keyword") String keyword,
			@Param("searchedAt") LocalDateTime searchedAt
	);

	@Query("""
			select history
			from SearchHistory history
			where history.user.id = :userId
				and history.deletedAt is null
			order by history.lastSearchedAt desc, history.id desc
			""")
	List<SearchHistory> findActiveByUserId(
			@Param(USER_ID_PARAM) Long userId,
			Pageable pageable
	);

	@Query("""
			select history
			from SearchHistory history
			where history.user.id = :userId
				and history.deletedAt is null
				and (
					history.lastSearchedAt < :lastSearchedAt
					or (history.lastSearchedAt = :lastSearchedAt and history.id < :cursorId)
				)
			order by history.lastSearchedAt desc, history.id desc
			""")
	List<SearchHistory> findActiveByUserIdAfter(
			@Param(USER_ID_PARAM) Long userId,
			@Param("lastSearchedAt") LocalDateTime lastSearchedAt,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);

	@Modifying
	@Query(value = """
			UPDATE search_histories
			SET deleted_at = :deletedAt, updated_at = :deletedAt
			WHERE search_history_id = :searchHistoryId
				AND user_id = :userId
				AND deleted_at IS NULL
			""", nativeQuery = true)
	void softDeleteOne(
			@Param(USER_ID_PARAM) Long userId,
			@Param("searchHistoryId") Long searchHistoryId,
			@Param("deletedAt") LocalDateTime deletedAt
	);

	@Modifying
	@Query(value = """
			UPDATE search_histories
			SET deleted_at = :deletedAt, updated_at = :deletedAt
			WHERE user_id = :userId AND deleted_at IS NULL
			""", nativeQuery = true)
	void softDeleteAllActiveByUserId(
			@Param("userId") Long userId,
			@Param("deletedAt") LocalDateTime deletedAt
	);
}

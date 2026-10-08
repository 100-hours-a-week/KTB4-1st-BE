package com.example.KTB_Agile_backend.search.service;

import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.search.dto.response.SearchHistoryPageResponse;
import com.example.KTB_Agile_backend.search.dto.response.SearchHistorySummary;
import com.example.KTB_Agile_backend.search.entity.SearchHistory;
import com.example.KTB_Agile_backend.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

	private static final int PAGE_SIZE = 10;
	private static final int FETCH_SIZE = PAGE_SIZE + 1;

	private final SearchHistoryRepository searchHistoryRepository;

	@Transactional
	public void recordProductSearch(Long userId, String keyword) {
		LocalDateTime searchedAt = LocalDateTime.now();
		searchHistoryRepository.upsertActiveKeyword(userId, keyword, searchedAt);
	}

	@Transactional(readOnly = true)
	public SearchHistoryPageResponse findByUser(Long userId, String cursor) {
		CursorCodec.TimeIdCursor decodedCursor = CursorCodec.decodeTimeId(cursor);
		List<SearchHistory> fetched = decodedCursor == null
				? searchHistoryRepository.findActiveByUserId(userId, PageRequest.of(0, FETCH_SIZE))
				: searchHistoryRepository.findActiveByUserIdAfter(
						userId,
						decodedCursor.time(),
						decodedCursor.id(),
						PageRequest.of(0, FETCH_SIZE)
				);
		CursorPage<SearchHistory> page = CursorPage.from(
				fetched,
				PAGE_SIZE,
				history -> CursorCodec.encodeTimeId(history.getLastSearchedAt(), history.getId())
		);
		List<SearchHistorySummary> searchHistories = page.items().stream()
				.map(SearchHistoryService::toSummary)
				.toList();
		return new SearchHistoryPageResponse(searchHistories, page.nextCursor(), page.hasNext());
	}

	@Transactional
	public void deleteOne(Long userId, Long searchHistoryId) {
		searchHistoryRepository.softDeleteOne(userId, searchHistoryId, LocalDateTime.now());
	}

	@Transactional
	public void deleteAll(Long userId) {
		searchHistoryRepository.softDeleteAllActiveByUserId(userId, LocalDateTime.now());
	}

	private static SearchHistorySummary toSummary(SearchHistory history) {
		return new SearchHistorySummary(
				history.getId(),
				history.getKeyword(),
				history.getLastSearchedAt(),
				history.getCreatedAt()
		);
	}
}

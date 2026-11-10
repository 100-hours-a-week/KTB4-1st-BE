package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.item.repository.JdbcItemCoViewRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemCoViewRecommendationService {

	private static final Duration VIEW_WINDOW = Duration.ofDays(7);

	private final JdbcItemCoViewRecommendationRepository repository;

	@Transactional
	public void recordView(Long userId, Long itemId, LocalDateTime viewedAt) {
		repository.recordView(userId, itemId, viewedAt);
	}

	@Transactional
	public void refreshScores() {
		LocalDateTime calculatedAt = LocalDateTime.now();
		LocalDateTime cutoff = calculatedAt.minus(VIEW_WINDOW);
		repository.deleteUserItemViewsBefore(cutoff);
		repository.deleteScores();
		repository.insertScoresSince(cutoff, calculatedAt);
	}

	@Transactional(readOnly = true)
	public List<Long> findTopRelatedItemIds(Long itemId, int limit) {
		return repository.findTopRelatedItemIds(itemId, limit);
	}
}

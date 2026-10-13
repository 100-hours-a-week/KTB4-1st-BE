package com.example.KTB_Agile_backend.search.dto.ai;

import java.util.List;

public record RankCandidatesRequest(List<Candidate> candidates, String preferenceText) {
	public record Candidate(Long itemId, String title) {
	}
}

package com.example.KTB_Agile_backend.ai.messagerefine.dto.ai;

import java.util.List;

public record MessageRefineAiRequest(String message, List<Answer> answers) {
	public record Answer(String question, String answer) {
	}
}

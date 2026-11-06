package com.example.KTB_Agile_backend.ai.messagerefine.dto.ai;

import com.example.KTB_Agile_backend.user.dto.UserPreferenceAnswerOption;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageRefineAiRequest(String message, Styles styles) {
	public record Styles(
			@JsonProperty("CONVERSATION_STYLE") UserPreferenceAnswerOption conversationStyle,
			@JsonProperty("DESCRIPTION_STYLE") UserPreferenceAnswerOption descriptionStyle,
			@JsonProperty("OPINION_STYLE") UserPreferenceAnswerOption opinionStyle
	) {
	}
}

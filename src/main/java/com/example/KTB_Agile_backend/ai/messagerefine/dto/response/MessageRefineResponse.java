package com.example.KTB_Agile_backend.ai.messagerefine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageRefineResponse(
		@JsonProperty("refined_message") String refinedMessage
) {
}

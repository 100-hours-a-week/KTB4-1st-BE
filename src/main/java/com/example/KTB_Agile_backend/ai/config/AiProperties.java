package com.example.KTB_Agile_backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ai")
public record AiProperties(
		@DefaultValue("") String imageAnalysisUrl,
		@DefaultValue("") String textModerationUrl,
		@DefaultValue("") String itemPriceUrl,
		@DefaultValue("") String smartSearchUrl,
		@DefaultValue("5") long connectTimeoutSeconds,
		@DefaultValue("90") long readTimeoutSeconds
) {
}

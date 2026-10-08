package com.example.KTB_Agile_backend.health.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/health/ping")
public class HealthPingController {

	private final JdbcTemplate jdbcTemplate;
	private final RestClient restClient;
	private final String aiImageAnalysisUrl;

	public HealthPingController(
			JdbcTemplate jdbcTemplate,
			@Value("${ai.image-analysis-url:}") String aiImageAnalysisUrl
	) {
		this.jdbcTemplate = jdbcTemplate;
		this.aiImageAnalysisUrl = aiImageAnalysisUrl;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(3));
		requestFactory.setReadTimeout(Duration.ofSeconds(3));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@GetMapping("/mysql")
	public ResponseEntity<Map<String, String>> pingMysql() {
		try {
			jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			return ResponseEntity.ok(Map.of("status", "UP"));
		} catch (DataAccessException ignored) {
			return unavailable();
		}
	}

	@GetMapping("/fastapi")
	public ResponseEntity<Map<String, String>> pingFastApi() {
		if (aiImageAnalysisUrl.isBlank()) {
			return unavailable();
		}
		try {
			restClient.get()
					.uri(URI.create(aiImageAnalysisUrl).resolve("/health"))
					.retrieve()
					.toBodilessEntity();
			return ResponseEntity.ok(Map.of("status", "UP"));
		} catch (IllegalArgumentException | RestClientException ignored) {
			return unavailable();
		}
	}

	private static ResponseEntity<Map<String, String>> unavailable() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN"));
	}
}

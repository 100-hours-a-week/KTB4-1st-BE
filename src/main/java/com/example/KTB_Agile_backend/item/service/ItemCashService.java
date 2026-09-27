package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.item.entity.ItemCash;
import com.example.KTB_Agile_backend.item.repository.ItemCashRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ItemCashService {

	private static final Duration CACHE_TTL = Duration.ofHours(24);
	private static final int MAX_KEYWORD_LENGTH = 255;

	private final ItemCashRepository itemCashRepository;
	private final RestClient restClient;
	private final String aiEndpoint;

	public ItemCashService(
			ItemCashRepository itemCashRepository,
			@Value("${ai.item-price-url:}") String aiEndpoint,
			@Value("${ai.connect-timeout-seconds:5}") long connectTimeoutSeconds,
			@Value("${ai.read-timeout-seconds:90}") long readTimeoutSeconds
	) {
		this.itemCashRepository = itemCashRepository;
		this.aiEndpoint = aiEndpoint;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
		requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Transactional
	public Long resolveUnitPrice(
			String title,
			String content,
			String keyword,
			BigDecimal valueTolerance,
			BigDecimal tradeSpeed
	) {
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
		LocalDateTime now = LocalDateTime.now();
		if (normalizedKeyword != null && !normalizedKeyword.isEmpty()) {
			ItemCash cached = itemCashRepository.findByKeywordAndExpiresAtAfter(normalizedKeyword, now)
					.orElse(null);
			if (cached != null) {
				return cached.getUnitPrice();
			}
		}
		AiPriceResponse response = estimatePrice(title, content, normalizedKeyword, valueTolerance, tradeSpeed);
		if (response.keyword() == null || response.keyword().isBlank()
				|| response.unitPrice() == null || response.unitPrice() < 0) {
			throw estimationFailed(null);
		}

		String responseKeyword = response.keyword().strip();
		if (responseKeyword.length() > MAX_KEYWORD_LENGTH) {
			throw estimationFailed(null);
		}
		LocalDateTime expiresAt = LocalDateTime.now().plus(CACHE_TTL);
		ItemCash itemCash = itemCashRepository.findByKeyword(responseKeyword).orElse(null);
		if (itemCash == null) {
			itemCashRepository.save(new ItemCash(responseKeyword, response.unitPrice(), expiresAt));
		} else {
			itemCash.refresh(response.unitPrice(), expiresAt);
		}
		return response.unitPrice();
	}

	private AiPriceResponse estimatePrice(
			String title,
			String content,
			String keyword,
			BigDecimal valueTolerance,
			BigDecimal tradeSpeed
	) {
		if (aiEndpoint.isBlank()) {
			throw new ApiException(AiErrorCode.AI_ITEM_PRICE_ENDPOINT_NOT_CONFIGURED);
		}
		try {
			AiPriceResponse response = restClient.post()
					.uri(aiEndpoint)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new AiPriceRequest(title, content, keyword, null, valueTolerance, tradeSpeed))
					.retrieve()
					.body(AiPriceResponse.class);
			if (response == null) {
				throw estimationFailed(null);
			}
			return response;
		} catch (RestClientException exception) {
			throw estimationFailed(exception);
		}
	}

	private static ApiException estimationFailed(Throwable cause) {
		return new ApiException(AiErrorCode.AI_ITEM_PRICE_ESTIMATION_FAILED, List.of(), cause);
	}

	private record AiPriceRequest(
			String title,
			String content,
			String keyword,
			Long unitPrice,
			BigDecimal valueTolerance,
			BigDecimal tradeSpeed
	) {
	}

	private record AiPriceResponse(String keyword, Long unitPrice, Long minUnitPrice, Long maxUnitPrice) {
	}
}

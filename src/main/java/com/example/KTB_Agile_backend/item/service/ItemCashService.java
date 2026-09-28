package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.ai.exception.AiErrorCode;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.item.cache.ItemPriceCache;
import com.example.KTB_Agile_backend.item.dto.ai.ItemPriceEstimationRequest;
import com.example.KTB_Agile_backend.item.dto.ai.ItemPriceEstimationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
public class ItemCashService {

	private static final Duration CACHE_TTL = Duration.ofHours(24);

	private final ItemPriceCache itemPriceCache;
	private final ItemPriceRangeCalculator itemPriceRangeCalculator;
	private final RestClient restClient;
	private final String aiEndpoint;

	public ItemCashService(
			ItemPriceCache itemPriceCache,
			ItemPriceRangeCalculator itemPriceRangeCalculator,
			@Value("${ai.item-price-url:}") String aiEndpoint,
			@Value("${ai.connect-timeout-seconds:5}") long connectTimeoutSeconds,
			@Value("${ai.read-timeout-seconds:90}") long readTimeoutSeconds
	) {
		this.itemPriceCache = itemPriceCache;
		this.itemPriceRangeCalculator = itemPriceRangeCalculator;
		this.aiEndpoint = aiEndpoint;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
		requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Transactional
	public ItemPriceRangeCalculator.PriceRange resolvePrice(
			String title,
			String content,
			String keyword,
			BigDecimal valueGapToleranceScore,
			BigDecimal exchangeUrgencyScore
	) {
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
		if (normalizedKeyword != null) {
			var cachedUnitPrice = itemPriceCache.get(normalizedKeyword);
			if (cachedUnitPrice.isPresent()) {
				return itemPriceRangeCalculator.calculate(
						cachedUnitPrice.get(), valueGapToleranceScore, exchangeUrgencyScore);
			}
		}
		ItemPriceEstimationResponse response = estimatePrice(
				title, content, valueGapToleranceScore, exchangeUrgencyScore);
		if (response.keyword() == null || response.keyword().isBlank()
				|| response.unitPrice() == null || response.unitPrice() < 0
				|| response.minUnitPrice() == null || response.minUnitPrice() < 0
				|| response.maxUnitPrice() == null || response.maxUnitPrice() < response.unitPrice()
				|| response.minUnitPrice() > response.unitPrice()) {
			throw estimationFailed(null);
		}

		if (normalizedKeyword != null) {
			itemPriceCache.put(normalizedKeyword, response.unitPrice(), CACHE_TTL);
		}
		return new ItemPriceRangeCalculator.PriceRange(
				response.unitPrice(), response.minUnitPrice(), response.maxUnitPrice());
	}

	private ItemPriceEstimationResponse estimatePrice(
			String title,
			String content,
			BigDecimal valueGapToleranceScore,
			BigDecimal exchangeUrgencyScore
	) {
		if (aiEndpoint.isBlank()) {
			throw new ApiException(AiErrorCode.AI_ITEM_PRICE_ENDPOINT_NOT_CONFIGURED);
		}
		try {
			ItemPriceEstimationResponse response = restClient.post()
					.uri(aiEndpoint)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ItemPriceEstimationRequest(
							title, content, valueGapToleranceScore, exchangeUrgencyScore))
					.retrieve()
					.body(ItemPriceEstimationResponse.class);
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
}

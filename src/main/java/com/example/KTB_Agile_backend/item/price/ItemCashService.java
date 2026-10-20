package com.example.KTB_Agile_backend.item.price;

import com.example.KTB_Agile_backend.ai.config.AiEndpoints;
import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.item.price.dto.ItemPriceEstimationRequest;
import com.example.KTB_Agile_backend.item.price.dto.ItemPriceEstimationResponse;
import com.example.KTB_Agile_backend.item.price.cache.ItemPriceCache;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import com.example.KTB_Agile_backend.item.price.exception.PriceEstimationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemCashService {

	private final ItemPriceCache itemPriceCache;
	private final ItemPriceRangeCalculator itemPriceRangeCalculator;
	private final AiProperties aiProperties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public Optional<ItemPriceRangeCalculator.PriceRange> cachedPrice(
			String keyword,
			BigDecimal valueGapToleranceScore,
			BigDecimal exchangeUrgencyScore
	) {
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
		if (normalizedKeyword != null) {
			var cachedUnitPrice = itemPriceCache.get(normalizedKeyword);
			if (cachedUnitPrice.isPresent()) {
				return Optional.of(itemPriceRangeCalculator.calculate(
						cachedUnitPrice.get(), valueGapToleranceScore, exchangeUrgencyScore));
			}
		}
		return Optional.empty();
	}

	public ItemPriceEstimationResponse estimatePrice(ItemPriceEstimationRequested request) {
		if (aiProperties.baseUrl().isBlank()) {
			throw new PriceEstimationException(PriceEstimationFailure.CONFIGURATION);
		}
		String body;
		try {
			body = restClient.post()
					.uri(AiEndpoints.ITEM_PRICE)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ItemPriceEstimationRequest(request.title(), request.content(),
							request.valueGapToleranceScore(), request.exchangeUrgencyScore()))
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException exception) {
			int status = exception.getStatusCode().value();
			var failure = status == 408 || status == 429 || status >= 500
					? PriceEstimationFailure.TRANSIENT
					: PriceEstimationFailure.INPUT;
			throw new PriceEstimationException(failure, exception);
		} catch (IllegalArgumentException exception) {
			throw new PriceEstimationException(PriceEstimationFailure.CONFIGURATION, exception);
		} catch (RestClientException exception) {
			throw new PriceEstimationException(PriceEstimationFailure.TRANSIENT, exception);
		}
		if (body == null || body.isBlank()) {
			throw new PriceEstimationException(PriceEstimationFailure.RESPONSE_VALIDATION);
		}
		try {
			return objectMapper.readValue(body, ItemPriceEstimationResponse.class);
		} catch (JacksonException exception) {
			throw new PriceEstimationException(PriceEstimationFailure.RESPONSE_VALIDATION, exception);
		}
	}
}

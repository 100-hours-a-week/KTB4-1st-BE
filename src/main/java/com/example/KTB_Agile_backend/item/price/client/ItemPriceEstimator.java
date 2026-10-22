package com.example.KTB_Agile_backend.item.price.client;

import com.example.KTB_Agile_backend.ai.config.AiEndpoints;
import com.example.KTB_Agile_backend.ai.config.AiProperties;
import com.example.KTB_Agile_backend.item.price.PriceEstimationFailure;
import com.example.KTB_Agile_backend.item.price.dto.ItemPriceEstimationRequest;
import com.example.KTB_Agile_backend.item.price.dto.ItemPriceEstimationResponse;
import com.example.KTB_Agile_backend.item.price.exception.PriceEstimationException;
import com.example.KTB_Agile_backend.item.price.messaging.ItemPriceEstimationRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ItemPriceEstimator {

	private final AiProperties aiProperties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public ItemPriceEstimationResponse estimate(ItemPriceEstimationRequested request) {
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

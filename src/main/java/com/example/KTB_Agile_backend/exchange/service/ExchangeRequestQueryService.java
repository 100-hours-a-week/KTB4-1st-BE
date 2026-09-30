package com.example.KTB_Agile_backend.exchange.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestEditResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.OfferedItemResponse;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.exception.ExchangeErrorCode;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExchangeRequestQueryService {

	private final ExchangeRequestRepository exchangeRequestRepository;

	@Transactional(readOnly = true)
	public ExchangeRequestEditResponse findForEdit(Long exchangeRequestId, Long requesterId) {
		ExchangeRequest exchangeRequest = exchangeRequestRepository.findById(exchangeRequestId)
				.orElseThrow(() -> new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_NOT_FOUND));
		validateEditableRequest(exchangeRequest, requesterId);
		return new ExchangeRequestEditResponse(
				exchangeRequest.getId(),
				exchangeRequest.getItem().getId(),
				exchangeRequest.getRequestedQuantity(),
				exchangeRequest.getOfferedItems().stream()
						.map(offered -> new OfferedItemResponse(
								offered.getItem().getId(), offered.getQuantity())).toList(),
				exchangeRequest.getRequestedStatus()
		);
	}

	private static void validateEditableRequest(ExchangeRequest exchangeRequest, Long requesterId) {
		if (!exchangeRequest.getRequester().getId().equals(requesterId)) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_FORBIDDEN);
		}
		if (exchangeRequest.getRequestedStatus() != ExchangeRequestStatus.PENDING) {
			throw new ApiException(ExchangeErrorCode.EXCHANGE_REQUEST_UPDATE_CONFLICT);
		}
	}
}

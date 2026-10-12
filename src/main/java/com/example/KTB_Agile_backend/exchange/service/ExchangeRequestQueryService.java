package com.example.KTB_Agile_backend.exchange.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.common.pagination.CursorPage;
import com.example.KTB_Agile_backend.exchange.dto.response.CompletedExchangePageResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestEditResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.OfferedItemResponse;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequest;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.exception.ExchangeErrorCode;
import com.example.KTB_Agile_backend.exchange.repository.ExchangeRequestRepository;
import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.image.repository.ImageRepository;
import com.example.KTB_Agile_backend.image.service.ImageUrlResolver;
import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;
import com.example.KTB_Agile_backend.item.entity.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExchangeRequestQueryService {

	private final ExchangeRequestRepository exchangeRequestRepository;
	private final ImageRepository imageRepository;
	private final ImageUrlResolver imageUrlResolver;

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

	@Transactional(readOnly = true)
	public CompletedExchangePageResponse findCompletedExchanges(Long userId, String sizeValue, String cursorValue) {
		int size = parsePageSize(sizeValue);
		CursorCodec.TimeIdCursor cursor = parseCursor(cursorValue);
		List<ExchangeRequestRepository.CompletedExchangeCursor> fetched = cursor == null
				? exchangeRequestRepository.findCompletedExchangeCursors(
						userId, ExchangeRequestStatus.COMPLETED, PageRequest.of(0, size + 1))
				: exchangeRequestRepository.findCompletedExchangeCursorsAfter(
						userId, ExchangeRequestStatus.COMPLETED,
						cursor.time(), cursor.id(), PageRequest.of(0, size + 1));
		CursorPage<ExchangeRequestRepository.CompletedExchangeCursor> page = CursorPage.from(
				fetched, size, cursorData -> CursorCodec.encodeTimeId(
						cursorData.getUpdatedAt(), cursorData.getExchangeRequestId()));
		List<Long> ids = page.items().stream()
				.map(ExchangeRequestRepository.CompletedExchangeCursor::getExchangeRequestId).toList();
		Map<Long, ExchangeRequest> exchangesById = new HashMap<>();
		if (!ids.isEmpty()) {
			exchangeRequestRepository.findAllByIdIn(ids)
					.forEach(exchange -> exchangesById.put(exchange.getId(), exchange));
		}
		List<ExchangeRequest> exchanges = ids.stream().map(exchangesById::get).toList();
		Map<Long, String> thumbnails = findThumbnails(exchanges);

		return new CompletedExchangePageResponse(
				exchanges.stream().map(exchange -> toCompletedExchange(exchange, thumbnails)).toList(),
				page.nextCursor(), page.hasNext());
	}

	private static CursorCodec.TimeIdCursor parseCursor(String cursorValue) {
		try {
			return CursorCodec.decodeTimeId(cursorValue);
		} catch (ApiException exception) {
			throw new ApiException(ErrorCode.BAD_REQUEST, "size 또는 cursor 값이 올바르지 않습니다.", exception);
		}
	}

	private static int parsePageSize(String sizeValue) {
		try {
			int size = Integer.parseInt(sizeValue);
			if (size >= 1 && size <= 100) {
				return size;
			}
		} catch (NumberFormatException ignored) {
			// Return the endpoint-specific bad request below.
		}
		throw new ApiException(ErrorCode.BAD_REQUEST, "size 또는 cursor 값이 올바르지 않습니다.");
	}

	private Map<Long, String> findThumbnails(List<ExchangeRequest> exchanges) {
		List<Long> itemIds = exchanges.stream()
				.flatMap(exchange -> Stream.concat(
						Stream.of(exchange.getItem()),
						exchange.getOfferedItems().stream().map(offered -> offered.getItem())))
				.map(Item::getId).distinct().toList();
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> thumbnails = new HashMap<>();
		for (Image image : imageRepository.findThumbnailImagesByItemIds(itemIds)) {
			thumbnails.put(image.getItem().getId(), imageUrlResolver.resolve(image));
		}
		return thumbnails;
	}

	private static CompletedExchangePageResponse.CompletedExchange toCompletedExchange(
			ExchangeRequest exchange, Map<Long, String> thumbnails
	) {
		return new CompletedExchangePageResponse.CompletedExchange(
				exchange.getId(), exchange.getRequestedStatus(), exchange.getUpdatedAt(),
				toExchangedItem(exchange.getItem(), exchange.getRequestedQuantity(), thumbnails),
				exchange.getOfferedItems().stream()
						.map(offered -> toExchangedItem(offered.getItem(), offered.getQuantity(), thumbnails)).toList());
	}

	private static CompletedExchangePageResponse.ExchangedItem toExchangedItem(
			Item item, Integer quantity, Map<Long, String> thumbnails
	) {
		return new CompletedExchangePageResponse.ExchangedItem(
				item.getId(), item.getTitle(), new ItemSummary.Owner(item.getUser().getId(), item.getUser().getNickname()),
				quantity, item.getItemState(), thumbnails.get(item.getId()));
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

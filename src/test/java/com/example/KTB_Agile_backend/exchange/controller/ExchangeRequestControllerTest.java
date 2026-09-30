package com.example.KTB_Agile_backend.exchange.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.exchange.exception.ExchangeErrorCode;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestCreatedResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestEditResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.OfferedItemResponse;
import com.example.KTB_Agile_backend.exchange.dto.response.ExchangeRequestStatusResponse;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.service.ExchangeRequestService;
import com.example.KTB_Agile_backend.exchange.service.ExchangeRequestQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExchangeRequestControllerTest {

	private ExchangeRequestService service;
	private ExchangeRequestQueryService queryService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(ExchangeRequestService.class);
		queryService = mock(ExchangeRequestQueryService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ExchangeRequestController(service, queryService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void createsRequestAndReturnsChatRoomId() throws Exception {
		when(service.create(eq(42L), eq(123L), any())).thenReturn(new ExchangeRequestCreatedResponse(
				301L, 123L, 1, List.of(new OfferedItemResponse(213L, 2)),
				ExchangeRequestStatus.PENDING, 401L, LocalDateTime.parse("2026-09-05T11:00:00")));

		mockMvc.perform(post("/items/123/exchange-requests")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"requestedQuantity":1,"offeredItems":[{"itemId":213,"quantity":2}]}
							"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/exchange-requests/301"))
				.andExpect(jsonPath("$.data.exchangeRequestId").value(301))
				.andExpect(jsonPath("$.data.requestedStatus").value("PENDING"))
				.andExpect(jsonPath("$.data.offeredItems[0].quantity").value(2))
				.andExpect(jsonPath("$.data.chatRoomId").value(401));

		verify(service).create(eq(42L), eq(123L), any());
	}

	@Test
	void returnsPendingRequestValuesForEditing() throws Exception {
		when(queryService.findForEdit(301L, 42L)).thenReturn(new ExchangeRequestEditResponse(
				301L, 123L, 2,
				List.of(new OfferedItemResponse(213L, 3)),
				ExchangeRequestStatus.PENDING));

		mockMvc.perform(get("/exchange-requests/301")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.exchangeRequestId").value(301))
				.andExpect(jsonPath("$.data.itemId").value(123))
				.andExpect(jsonPath("$.data.requestedQuantity").value(2))
				.andExpect(jsonPath("$.data.offeredItems[0].itemId").value(213))
				.andExpect(jsonPath("$.data.offeredItems[0].quantity").value(3))
				.andExpect(jsonPath("$.data.requestedStatus").value("PENDING"));

		verify(queryService).findForEdit(301L, 42L);
	}

	@Test
	void updatesRequestAndReturnsNoContent() throws Exception {
		mockMvc.perform(put("/exchange-requests/301")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"requestedQuantity":2,"offeredItems":[{"itemId":213,"quantity":3}]}
							"""))
				.andExpect(status().isNoContent());

		verify(service).update(eq(301L), eq(42L), any());
	}

	@Test
	void cancelsRequestWithoutBodyAndReturnsNoContent() throws Exception {
		for (String path : List.of("/exchange-requests/301", "/api/exchange-requests/301")) {
			mockMvc.perform(delete(path)
						.principal(new UsernamePasswordAuthenticationToken("42", null)))
					.andExpect(status().isNoContent())
					.andExpect(content().string(""));
		}
		verify(service, org.mockito.Mockito.times(2)).cancel(301L, 42L);
	}

	@Test
	void rejectsInvalidCancellationIds() throws Exception {
		for (String id : List.of("abc", "0", "-1", "9223372036854775808")) {
			mockMvc.perform(delete("/api/exchange-requests/" + id)
						.principal(new UsernamePasswordAuthenticationToken("42", null)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.data").isEmpty())
					.andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
					.andExpect(jsonPath("$.error.message").value("교환 요청 ID가 올바르지 않습니다."))
					.andExpect(jsonPath("$.error.details").isEmpty());
		}
	}

	@Test
	void returnsCancellationErrors() throws Exception {
		for (ExchangeErrorCode code : List.of(ExchangeErrorCode.EXCHANGE_REQUEST_CANCEL_FORBIDDEN,
				ExchangeErrorCode.EXCHANGE_REQUEST_CANCEL_NOT_FOUND,
				ExchangeErrorCode.EXCHANGE_REQUEST_CANCEL_CONFLICT)) {
			doThrow(new ApiException(code)).when(service).cancel(301L, 42L);
			mockMvc.perform(delete("/api/exchange-requests/301")
						.principal(new UsernamePasswordAuthenticationToken("42", null)))
					.andExpect(status().is(code.status().value()))
					.andExpect(jsonPath("$.data").isEmpty())
					.andExpect(jsonPath("$.error.code").value(code.value()))
					.andExpect(jsonPath("$.error.message").value(code.message()))
					.andExpect(jsonPath("$.error.details").isEmpty());
		}
		doThrow(new IllegalStateException("database failure")).when(service).cancel(301L, 42L);
		mockMvc.perform(delete("/api/exchange-requests/301").servletPath("/api/exchange-requests/301")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."));
	}

	@Test
	void rejectsInvalidNumericInputAsBadRequest() throws Exception {
		mockMvc.perform(post("/items/123/exchange-requests")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"requestedQuantity":1.5,"offeredItems":[{"itemId":213,"quantity":2}]}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("EXCHANGE_REQUEST_CREATE_INVALID"))
				.andExpect(jsonPath("$.error.message")
						.value("교환 요청의 itemId, requestedQuantity 또는 offeredItems 형식이 올바르지 않습니다."));
	}

	@Test
	void rejectsStringAndOutOfRangeQuantities() throws Exception {
		for (String quantity : List.of("\"1\"", "2147483648")) {
			mockMvc.perform(post("/items/123/exchange-requests")
						.principal(new UsernamePasswordAuthenticationToken("42", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestedQuantity\":" + quantity
								+ ",\"offeredItems\":[{\"itemId\":213,\"quantity\":2}]}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("EXCHANGE_REQUEST_CREATE_INVALID"));
		}
	}

	@Test
	void rejectsMissingRequiredFields() throws Exception {
		mockMvc.perform(post("/items/123/exchange-requests")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"requestedQuantity\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.message")
						.value("교환 요청의 itemId, requestedQuantity 또는 offeredItems 형식이 올바르지 않습니다."));
	}

	@Test
	void updatesRequestStatus() throws Exception {
		when(service.updateStatus(301L, 42L, ExchangeRequestStatus.COMPLETED)).thenReturn(
				new ExchangeRequestStatusResponse(301L, 123L, ExchangeRequestStatus.COMPLETED,
						LocalDateTime.parse("2026-09-05T16:00:00")));

		mockMvc.perform(patch("/exchange-requests/301/status")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"COMPLETED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.exchangeRequestId").value(301))
				.andExpect(jsonPath("$.data.status").value("COMPLETED"));

		verify(service).updateStatus(301L, 42L, ExchangeRequestStatus.COMPLETED);

		when(service.updateStatus(301L, 42L, ExchangeRequestStatus.CANCELED)).thenReturn(
				new ExchangeRequestStatusResponse(301L, 123L, ExchangeRequestStatus.CANCELED,
						LocalDateTime.parse("2026-09-05T16:00:00")));
		mockMvc.perform(patch("/exchange-requests/301/status")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"CANCELED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CANCELED"));

		verify(service).updateStatus(301L, 42L, ExchangeRequestStatus.CANCELED);
	}

	@Test
	void rejectsPendingStatusAsInvalidPatchValue() throws Exception {
		mockMvc.perform(patch("/exchange-requests/301/status")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"PENDING\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("EXCHANGE_REQUEST_STATUS_INVALID"))
				.andExpect(jsonPath("$.error.details[0].field").value("status"))
				.andExpect(jsonPath("$.error.details[0].reason")
						.value("COMPLETED, REJECTED 또는 CANCELED만 입력해 주세요."));
	}
}

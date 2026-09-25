package com.example.KTB_Agile_backend.exchange.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestCreatedResponse;
import com.example.KTB_Agile_backend.exchange.api.ExchangeRequestStatusResponse;
import com.example.KTB_Agile_backend.exchange.entity.ExchangeRequestStatus;
import com.example.KTB_Agile_backend.exchange.service.ExchangeRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExchangeRequestControllerTest {

	private ExchangeRequestService service;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(ExchangeRequestService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ExchangeRequestController(service))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void createsRequestAndReturnsLocationAndNullChatRoom() throws Exception {
		when(service.create(eq(42L), eq(123L), any())).thenReturn(new ExchangeRequestCreatedResponse(
				301L, 123L, 1, List.of(new ExchangeRequestCreatedResponse.OfferedItemResponse(213L, 2)),
				ExchangeRequestStatus.PENDING, null, OffsetDateTime.parse("2026-09-05T11:00:00+09:00")));

		mockMvc.perform(post("/api/items/123/exchange-requests")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"requestedQuantity":1,"offeredItems":[{"itemId":213,"quantity":2}]}
							"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/exchange-requests/301"))
				.andExpect(jsonPath("$.data.exchangeRequestId").value(301))
				.andExpect(jsonPath("$.data.requestedStatus").value("PENDING"))
				.andExpect(jsonPath("$.data.offeredItems[0].quantity").value(2))
				.andExpect(jsonPath("$.data.chatRoomId").value(org.hamcrest.Matchers.nullValue()));

		verify(service).create(eq(42L), eq(123L), any());
	}

	@Test
	void rejectsInvalidNumericInputAsBadRequest() throws Exception {
		mockMvc.perform(post("/api/items/123/exchange-requests")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"requestedQuantity":1.5,"offeredItems":[{"itemId":213,"quantity":2}]}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.error.message")
						.value("교환 요청의 itemId, requestedQuantity 또는 offeredItems 형식이 올바르지 않습니다."));
	}

	@Test
	void rejectsStringAndOutOfRangeQuantities() throws Exception {
		for (String quantity : List.of("\"1\"", "2147483648")) {
			mockMvc.perform(post("/api/items/123/exchange-requests")
						.principal(new UsernamePasswordAuthenticationToken("42", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestedQuantity\":" + quantity
								+ ",\"offeredItems\":[{\"itemId\":213,\"quantity\":2}]}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
		}
	}

	@Test
	void rejectsMissingRequiredFields() throws Exception {
		mockMvc.perform(post("/api/items/123/exchange-requests")
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
						OffsetDateTime.parse("2026-09-05T16:00:00+09:00")));

		mockMvc.perform(patch("/api/exchange-requests/301/status")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"COMPLETED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.exchangeRequestId").value(301))
				.andExpect(jsonPath("$.data.status").value("COMPLETED"));

		verify(service).updateStatus(301L, 42L, ExchangeRequestStatus.COMPLETED);
	}

	@Test
	void rejectsPendingStatusAsInvalidPatchValue() throws Exception {
		mockMvc.perform(patch("/api/exchange-requests/301/status")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"PENDING\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.error.details[0].field").value("status"))
				.andExpect(jsonPath("$.error.details[0].reason").value("COMPLETED 또는 REJECTED만 입력해 주세요."));
	}
}

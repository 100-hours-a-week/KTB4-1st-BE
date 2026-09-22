package com.example.KTB_Agile_backend.item.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.item.dto.response.ItemCreateResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemPageResponse;
import com.example.KTB_Agile_backend.item.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerTest {

	private ItemService itemService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		itemService = mock(ItemService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ItemController(itemService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void createsItemWithItemIdAndCreatedStatus() throws Exception {
		when(itemService.create(eq(42L), any())).thenReturn(new ItemCreateResponse(123L));

		mockMvc.perform(post("/items")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content(payload()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.itemId").value(123));

		verify(itemService).create(eq(42L), any());
	}

	@Test
	void listsItemsByGroupWithCursor() throws Exception {
		when(itemService.findByGroup(42L, 7L, "cursor"))
				.thenReturn(new ItemPageResponse(List.of(), null, false));

		mockMvc.perform(get("/groups/7/items")
					.param("cursor", "cursor")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.hasNext").value(false))
				.andExpect(jsonPath("$.data.nextCursor").doesNotExist());

		verify(itemService).findByGroup(42L, 7L, "cursor");
	}

	private static String payload() {
		return """
				{
				  "title": "임시 제목입니다.",
				  "content": "임시 내용입니다.",
				  "quantity": 3,
				  "itemState": "AVAILABLE",
				  "exchangeUrgencyScore": 0.50,
				  "valueGapToleranceScore": 0.30,
				  "groupIds": [101, 205],
				  "imageIds": [1001, 1002]
				}
				""";
	}
}

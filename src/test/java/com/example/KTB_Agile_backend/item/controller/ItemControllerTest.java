package com.example.KTB_Agile_backend.item.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.item.dto.request.UpdateItemRequest;
import com.example.KTB_Agile_backend.item.dto.response.ItemCreateResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemDetailResponse;
import com.example.KTB_Agile_backend.item.dto.response.ItemPageResponse;
import com.example.KTB_Agile_backend.item.entity.ItemState;
import com.example.KTB_Agile_backend.item.service.ItemService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
	void updatesItemWithNoContent() throws Exception {
		mockMvc.perform(put("/items/123")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content(payload()))
				.andExpect(status().isNoContent());

		verify(itemService).update(eq(42L), eq(123L), any(UpdateItemRequest.class));
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

	@Test
	void getsItemDetail() throws Exception {
		when(itemService.findDetail(42L, 123L)).thenReturn(new ItemDetailResponse(
				123L,
				List.of(new ItemDetailResponse.GroupInfo(101L, "카테뷰")),
				"게시글 제목1",
				"게시글 내용입니다.",
				1,
				ItemState.AVAILABLE,
				new ItemDetailResponse.Owner(10L, "사용자1", "https://example.com/profile.jpg"),
				List.of(new ItemDetailResponse.ImageInfo(
						501L, "https://example.com/item1.jpg", 1)),
				33L,
				128L,
				2L,
				false,
				OffsetDateTime.parse("2026-09-04T13:30:00+09:00"),
				OffsetDateTime.parse("2026-09-04T13:30:00+09:00")
		));

		mockMvc.perform(get("/items/123")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.itemId").value(123))
				.andExpect(jsonPath("$.data.groups[0].groupName").value("카테뷰"))
				.andExpect(jsonPath("$.data.owner.nickname").value("사용자1"))
				.andExpect(jsonPath("$.data.images[0].displayOrder").value(1))
				.andExpect(jsonPath("$.data.likeCount").value(33))
				.andExpect(jsonPath("$.data.viewCount").value(128))
				.andExpect(jsonPath("$.data.isLiked").value(false));

		verify(itemService).findDetail(42L, 123L);
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

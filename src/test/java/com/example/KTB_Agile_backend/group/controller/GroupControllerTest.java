package com.example.KTB_Agile_backend.group.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.group.dto.response.GroupPageResponse;
import com.example.KTB_Agile_backend.group.dto.response.GroupSummary;
import com.example.KTB_Agile_backend.group.service.GroupQueryService;
import com.example.KTB_Agile_backend.group.service.GroupService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupControllerTest {

	private GroupService groupService;
	private GroupQueryService groupQueryService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		groupService = mock(GroupService.class);
		groupQueryService = mock(GroupQueryService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new GroupController(groupService, groupQueryService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void searchesGroups() throws Exception {
		when(groupQueryService.search(42L, "마을", null))
				.thenReturn(new GroupPageResponse(List.of(), null, false));

		mockMvc.perform(get("/groups")
					.param("keyword", "마을")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.groups").isArray())
				.andExpect(jsonPath("$.data.hasNext").value(false));

		verify(groupQueryService).search(42L, "마을", null);
	}

	@Test
	void recommendsGroups() throws Exception {
		when(groupQueryService.recommendations(42L, null)).thenReturn(new GroupPageResponse(
				List.of(new GroupSummary(
						101L,
						"분당 정자동 나눔방",
						"경기도 성남시 분당구 정자동 178-1",
						"정자동 주민들을 위한 물품 교환 그룹입니다.",
						24L,
						12L,
						LocalDateTime.of(2026, 9, 4, 13, 30),
						false
				)),
				"cursor",
				true
		));

		mockMvc.perform(get("/groups/recommendations")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.groups[0].groupId").value(101))
				.andExpect(jsonPath("$.data.groups[0].memberCount").value(24))
				.andExpect(jsonPath("$.data.groups[0].itemCount").value(12))
				.andExpect(jsonPath("$.data.groups[0].isJoined").value(false))
				.andExpect(jsonPath("$.data.nextCursor").value("cursor"))
				.andExpect(jsonPath("$.data.hasNext").value(true));

		verify(groupQueryService).recommendations(42L, null);
	}

	@Test
	void joinsGroup() throws Exception {
		mockMvc.perform(post("/groups/7/members")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/groups/7/members/me"));

		verify(groupService).join(42L, 7L);
	}

	@Test
	void leavesGroup() throws Exception {
		mockMvc.perform(delete("/groups/7/members/me")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isNoContent());

		verify(groupService).leave(42L, 7L);
	}

	@Test
	void createsGroup() throws Exception {
		mockMvc.perform(post("/groups")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "groupName": "우리 그룹",
							  "roadAddress": "서울시 중구 세종대로 1",
							  "longitude": 126.978,
							  "latitude": 37.5665,
							  "groupContent": "함께 거래해요"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(content().string(""));

		verify(groupService).create(eq(42L), any());
	}

	@Test
	void rejectsInvalidGroupRequest() throws Exception {
		mockMvc.perform(post("/groups")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "groupName": "",
							  "roadAddress": "주소",
							  "longitude": 126.978,
							  "latitude": 37.5665
							}
							"""))
				.andExpect(status().isBadRequest());
	}
}

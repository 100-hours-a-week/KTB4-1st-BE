package com.example.KTB_Agile_backend.group.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.group.dto.response.GroupCreatedResponse;
import com.example.KTB_Agile_backend.group.dto.response.GroupPageResponse;
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
		when(groupQueryService.search("마을", null))
				.thenReturn(new GroupPageResponse(List.of(), 20, false, null));

		mockMvc.perform(get("/groups")
					.param("keyword", "마을")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.groups").isArray())
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.hasNext").value(false));

		verify(groupQueryService).search("마을", null);
	}

	@Test
	void joinsGroup() throws Exception {
		mockMvc.perform(post("/groups/7/members")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isNoContent());

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
		when(groupService.create(eq(42L), any())).thenReturn(new GroupCreatedResponse(
				7L,
				LocalDateTime.of(2026, 9, 20, 12, 0)
		));

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
				.andExpect(header().string("Location", "/groups/7"))
				.andExpect(jsonPath("$.data.groupId").value(7))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-20T12:00:00"))
				.andExpect(jsonPath("$.error").doesNotExist());

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

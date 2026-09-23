package com.example.KTB_Agile_backend.user.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.group.dto.response.GroupPageResponse;
import com.example.KTB_Agile_backend.group.service.GroupQueryService;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceAnswerOption;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceQuestion;
import com.example.KTB_Agile_backend.user.dto.response.UserPreferenceResponse;
import com.example.KTB_Agile_backend.user.service.AccountWithdrawalService;
import com.example.KTB_Agile_backend.user.service.UserPreferenceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

	private AccountWithdrawalService accountWithdrawalService;
	private UserPreferenceService userPreferenceService;
	private GroupQueryService groupQueryService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		accountWithdrawalService = mock(AccountWithdrawalService.class);
		userPreferenceService = mock(UserPreferenceService.class);
		groupQueryService = mock(GroupQueryService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new UserController(
						accountWithdrawalService,
						userPreferenceService,
						groupQueryService
				))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void withdrawsAuthenticatedUserAndReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/users")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isNoContent());

		verify(accountWithdrawalService).withdraw(42L);
	}

	@Test
	void getsMyGroupsForAuthenticatedUser() throws Exception {
		when(groupQueryService.myGroups(42L)).thenReturn(new GroupPageResponse(List.of(), null, false));

		mockMvc.perform(get("/users/me/groups")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.groups").isArray())
				.andExpect(jsonPath("$.data.hasNext").value(false));

		verify(groupQueryService).myGroups(42L);
	}

	@Test
	void createsPreferencesAndReturnsCreatedPreference() throws Exception {
		when(userPreferenceService.create(eq(42L), any())).thenReturn(new UserPreferenceResponse(
				123L,
				List.of(new UserPreferenceResponse.Answer(
						UserPreferenceQuestion.CONVERSATION_STYLE,
						UserPreferenceAnswerOption.CONCISE
				)),
				LocalDateTime.of(2026, 9, 4, 15, 30)
		));

		mockMvc.perform(post("/users/preferences")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
								"answers": [
									{"question": "CONVERSATION_STYLE", "answer": "CONCISE"},
									{"question": "DESCRIPTION_STYLE", "answer": "BRIEF"},
									{"question": "OPINION_STYLE", "answer": "CLEAR"}
								  ]
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.userPreferenceId").value(123))
				.andExpect(jsonPath("$.data.answers[0].question").value("CONVERSATION_STYLE"))
				.andExpect(jsonPath("$.data.answers[0].answer").value("CONCISE"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-04T15:30:00"))
				.andExpect(jsonPath("$.error").doesNotExist());

		verify(userPreferenceService).create(eq(42L), any());
	}

	@Test
	void updatesPreferencesAndReturnsUpdatedPreference() throws Exception {
		when(userPreferenceService.update(eq(42L), any())).thenReturn(new UserPreferenceResponse(
				123L,
				List.of(new UserPreferenceResponse.Answer(
						UserPreferenceQuestion.DESCRIPTION_STYLE,
						UserPreferenceAnswerOption.DETAILED
				)),
				LocalDateTime.of(2026, 9, 4, 15, 30)
		));

		mockMvc.perform(put("/users/preferences")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
								{
								  "answers": [
									{"question": "CONVERSATION_STYLE", "answer": "CONCISE"},
									{"question": "DESCRIPTION_STYLE", "answer": "DETAILED"},
									{"question": "OPINION_STYLE", "answer": "CLEAR"}
								  ]
								}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userPreferenceId").value(123))
				.andExpect(jsonPath("$.data.answers[0].answer").value("DETAILED"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-04T15:30:00"))
				.andExpect(jsonPath("$.error").doesNotExist());

		verify(userPreferenceService).update(eq(42L), any());
	}

	@Test
	void getsPreferencesForAuthenticatedUser() throws Exception {
		when(userPreferenceService.get(42L)).thenReturn(new UserPreferenceResponse(
				123L,
				List.of(new UserPreferenceResponse.Answer(
						UserPreferenceQuestion.CONVERSATION_STYLE,
						UserPreferenceAnswerOption.CONCISE
				)),
				LocalDateTime.of(2026, 9, 4, 15, 30)
		));

		mockMvc.perform(get("/users/preferences")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userPreferenceId").value(123))
				.andExpect(jsonPath("$.data.answers[0].question").value("CONVERSATION_STYLE"))
				.andExpect(jsonPath("$.data.answers[0].answer").value("CONCISE"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-04T15:30:00"));

		verify(userPreferenceService).get(42L);
	}

	@Test
	void rejectsUnknownPreferenceValue() throws Exception {
		mockMvc.perform(post("/users/preferences")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "answers": [{"question": "conversationStyle", "answer": "concise"}]
							}
							"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsIncompletePreferenceQuestions() throws Exception {
		mockMvc.perform(post("/users/preferences")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "answers": [
								{"question": "CONVERSATION_STYLE", "answer": "CONCISE"},
								{"question": "DESCRIPTION_STYLE", "answer": "BRIEF"}
							  ]
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.details[0].reason")
						.value("질문 3개를 모두 입력해야 합니다."));
	}
}

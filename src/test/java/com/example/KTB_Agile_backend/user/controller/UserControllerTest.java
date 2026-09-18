package com.example.KTB_Agile_backend.user.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

	private AccountWithdrawalService accountWithdrawalService;
	private UserPreferenceService userPreferenceService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		accountWithdrawalService = mock(AccountWithdrawalService.class);
		userPreferenceService = mock(UserPreferenceService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new UserController(accountWithdrawalService, userPreferenceService))
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
								{"question": "DESCRIPTION_STYLE", "answer": "BRIEF"}
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
	void updatesPreferencesAndReturnsNoContent() throws Exception {
		mockMvc.perform(put("/users/preferences")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "answers": [{"question": "DESCRIPTION_STYLE", "answer": "DETAILED"}]
							}
							"""))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

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
				.andExpect(jsonPath("$.userPreferenceId").value(123))
				.andExpect(jsonPath("$.answers[0].question").value("CONVERSATION_STYLE"))
				.andExpect(jsonPath("$.answers[0].answer").value("CONCISE"))
				.andExpect(jsonPath("$.createdAt").value("2026-09-04T15:30:00"));

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
}

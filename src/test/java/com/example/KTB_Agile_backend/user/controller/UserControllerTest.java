package com.example.KTB_Agile_backend.user.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.user.service.AccountWithdrawalService;
import com.example.KTB_Agile_backend.user.service.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
	void createsPreferencesWithoutReturningAiOnlyValues() throws Exception {
		mockMvc.perform(post("/users/preferences")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "answers": [
								{"question": "conversationStyle", "answer": "concise"},
								{"question": "descriptionStyle", "answer": "brief"}
							  ]
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(content().string(""));

		verify(userPreferenceService).create(eq(42L), any());
	}
}

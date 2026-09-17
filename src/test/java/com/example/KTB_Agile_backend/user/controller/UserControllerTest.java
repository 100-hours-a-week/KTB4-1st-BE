package com.example.KTB_Agile_backend.user.controller;

import com.example.KTB_Agile_backend.user.service.AccountWithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

	private AccountWithdrawalService accountWithdrawalService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		accountWithdrawalService = mock(AccountWithdrawalService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new UserController(accountWithdrawalService))
				.build();
	}

	@Test
	void withdrawsAuthenticatedUserAndReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/users")
					.principal(new UsernamePasswordAuthenticationToken("42", null)))
				.andExpect(status().isNoContent());

		verify(accountWithdrawalService).withdraw(42L);
	}
}

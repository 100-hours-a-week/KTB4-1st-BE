package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import com.example.KTB_Agile_backend.user.repository.RefreshTokenRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountWithdrawalServiceTest {

	@Test
	void withdrawsUserAndRevokesAllRefreshTokens() {
		UserRepository userRepository = mock(UserRepository.class);
		RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
		AccountWithdrawalService service = new AccountWithdrawalService(
				userRepository,
				refreshTokenRepository
		);
		User user = new User("kim");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		service.withdraw(1L);

		assertEquals(UserStatus.WITHDRAWN, user.getUserStatus());
		assertNotNull(user.getDeletedAt());
		verify(refreshTokenRepository).revokeAllByUserId(eq(1L), any(LocalDateTime.class));
	}
}

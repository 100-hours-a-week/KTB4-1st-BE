package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountWithdrawalServiceTest {

	@Test
	void marksUserWithdrawn() {
		UserRepository userRepository = mock(UserRepository.class);
		AccountWithdrawalService service = new AccountWithdrawalService(userRepository);
		User user = new User("kim");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		service.withdraw(1L);

		assertEquals(UserStatus.WITHDRAWN, user.getUserStatus());
		assertNotNull(user.getDeletedAt());
	}
}

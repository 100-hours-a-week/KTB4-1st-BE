package com.example.KTB_Agile_backend.user.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UserTests {

	@Test
	void createsActiveUserWithDefaultRole() {
		User user = new User("nickname");

		assertThat(user.getNickname()).isEqualTo("nickname");
		assertThat(user.getUserRole()).isEqualTo(UserRole.USER);
		assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void rejectsBlankNickname() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User(" "));
	}

	@Test
	void rejectsNicknameLongerThanColumnLimit() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User("a".repeat(201)));
	}

	@Test
	void withdrawsUser() {
		User user = new User("nickname");
		LocalDateTime withdrawnAt = LocalDateTime.of(2026, 9, 16, 12, 0);

		user.withdraw(withdrawnAt);

		assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(user.getDeletedAt()).isEqualTo(withdrawnAt);
	}
}

package com.example.KTB_Agile_backend.user.entity;

import org.junit.jupiter.api.Test;

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
}

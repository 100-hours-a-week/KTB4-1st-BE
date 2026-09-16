package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.SocialAccount;
import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class SocialAccountRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Test
	void rejectsDuplicateProviderAccount() {
		User firstUser = userRepository.saveAndFlush(new User("first"));
		User secondUser = userRepository.saveAndFlush(new User("second"));
		socialAccountRepository.saveAndFlush(
				new SocialAccount(firstUser, "KAKAO", "provider-user-1")
		);

		assertThrows(DataIntegrityViolationException.class, () ->
				socialAccountRepository.saveAndFlush(
						new SocialAccount(secondUser, "KAKAO", "provider-user-1")
				)
		);
	}
}

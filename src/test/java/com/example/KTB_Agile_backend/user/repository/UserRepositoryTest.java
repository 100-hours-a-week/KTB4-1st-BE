package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	void storesProfileImageUrlLongerThan255Characters() {
		String profileImageUrl = "https://example.com/" + "a".repeat(300);
		User user = userRepository.saveAndFlush(new User("nickname", profileImageUrl));
		entityManager.clear();

		assertThat(userRepository.findById(user.getId()).orElseThrow().getProfileImageUrl())
				.isEqualTo(profileImageUrl);
	}
}

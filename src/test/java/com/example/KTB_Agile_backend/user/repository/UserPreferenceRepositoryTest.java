package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserPreference;
import com.example.KTB_Agile_backend.user.entity.UserPreferenceAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class UserPreferenceRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserPreferenceRepository userPreferenceRepository;

	@Test
	void savesVariableAnswersAndFindsPreferenceByUser() {
		User user = userRepository.saveAndFlush(new User("nickname"));
		userPreferenceRepository.saveAndFlush(new UserPreference(
				user,
				List.of(
						new UserPreferenceAnswer("question-one", "answer-one"),
						new UserPreferenceAnswer("question-two", "answer-two")
				)
		));

		UserPreference preference = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow();

		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
		assertThat(preference.getCreatedAt()).isNotNull();
		assertThat(userPreferenceRepository.existsByUser_Id(user.getId())).isTrue();
		assertThat(preference.getAnswers()).extracting(UserPreferenceAnswer::getQuestion)
				.containsExactly("question-one", "question-two");
	}

	@Test
	void rejectsSecondPreferenceForSameUser() {
		User user = userRepository.saveAndFlush(new User("nickname"));
		userPreferenceRepository.saveAndFlush(new UserPreference(
				user,
				List.of(new UserPreferenceAnswer("question", "answer"))
		));

		assertThrows(DataIntegrityViolationException.class, () ->
				userPreferenceRepository.saveAndFlush(new UserPreference(
						user,
						List.of(new UserPreferenceAnswer("other-question", "other-answer"))
				))
		);
	}
}

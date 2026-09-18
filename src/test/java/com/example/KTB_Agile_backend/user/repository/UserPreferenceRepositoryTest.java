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
						new UserPreferenceAnswer("CONVERSATION_STYLE", "CONCISE"),
						new UserPreferenceAnswer("DESCRIPTION_STYLE", "BRIEF")
				)
		));

		UserPreference preference = userPreferenceRepository.findByUser_Id(user.getId()).orElseThrow();

		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
		assertThat(preference.getCreatedAt()).isNotNull();
		assertThat(userPreferenceRepository.existsByUser_Id(user.getId())).isTrue();
		assertThat(preference.getAnswers()).extracting(UserPreferenceAnswer::getQuestion)
				.containsExactly("CONVERSATION_STYLE", "DESCRIPTION_STYLE");
	}

	@Test
	void rejectsSecondPreferenceForSameUser() {
		User user = userRepository.saveAndFlush(new User("nickname"));
		userPreferenceRepository.saveAndFlush(new UserPreference(
				user,
				List.of(new UserPreferenceAnswer("CONVERSATION_STYLE", "CONCISE"))
		));

		assertThrows(DataIntegrityViolationException.class, () ->
				userPreferenceRepository.saveAndFlush(new UserPreference(
						user,
						List.of(new UserPreferenceAnswer("DESCRIPTION_STYLE", "BRIEF"))
				))
		);
	}

	@Test
	void rejectsUnknownQuestionAtDatabase() {
		User user = userRepository.saveAndFlush(new User("nickname"));

		assertThrows(DataIntegrityViolationException.class, () ->
				userPreferenceRepository.saveAndFlush(new UserPreference(
						user,
						List.of(new UserPreferenceAnswer("UNKNOWN", "CONCISE"))
				))
		);
	}
}

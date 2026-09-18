package com.example.KTB_Agile_backend.user.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferenceTests {

	@Test
	void keepsAnyNumberOfQuestionAnswers() {
		UserPreference preference = new UserPreference(
				new User("nickname"),
				List.of(
						new UserPreferenceAnswer("CONVERSATION_STYLE", "CONCISE"),
						new UserPreferenceAnswer("DESCRIPTION_STYLE", "BRIEF"),
						new UserPreferenceAnswer("OPINION_STYLE", "CLEAR")
				)
		);

		assertThat(preference.getAnswers()).hasSize(3);
		assertThat(preference.getAnswers().get(2).getAnswer()).isEqualTo("CLEAR");
	}
}

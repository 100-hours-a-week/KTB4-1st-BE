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
						new UserPreferenceAnswer("conversationStyle", "concise"),
						new UserPreferenceAnswer("descriptionStyle", "brief"),
						new UserPreferenceAnswer("futureQuestion", "futureAnswer")
				)
		);

		assertThat(preference.getAnswers()).hasSize(3);
		assertThat(preference.getAnswers().get(2).getAnswer()).isEqualTo("futureAnswer");
	}
}

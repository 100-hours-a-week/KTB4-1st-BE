package com.example.KTB_Agile_backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceAnswer {

	@Column(nullable = false, length = 1000)
	private String question;

	@Column(nullable = false, length = 1000)
	private String answer;

	public UserPreferenceAnswer(String question, String answer) {
		this.question = question;
		this.answer = answer;
	}
}

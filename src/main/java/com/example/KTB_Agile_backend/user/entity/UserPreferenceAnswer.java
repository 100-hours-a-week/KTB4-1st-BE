package com.example.KTB_Agile_backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceAnswer {

	@Column(
			nullable = false,
			length = 1000,
			check = @CheckConstraint(
					name = "ck_user_preference_question",
					constraint = "CASE question WHEN 'CONVERSATION_STYLE' THEN TRUE WHEN 'DESCRIPTION_STYLE' THEN TRUE WHEN 'OPINION_STYLE' THEN TRUE ELSE FALSE END"
			)
	)
	private String question;

	@Column(
			nullable = false,
			length = 1000,
			check = @CheckConstraint(
					name = "ck_user_preference_answer",
					constraint = "CASE answer WHEN 'CONCISE' THEN TRUE WHEN 'COMFORTABLE' THEN TRUE WHEN 'WARM' THEN TRUE WHEN 'BRIEF' THEN TRUE WHEN 'MODERATE' THEN TRUE WHEN 'DETAILED' THEN TRUE WHEN 'CLEAR' THEN TRUE WHEN 'NATURAL' THEN TRUE WHEN 'INDIRECT' THEN TRUE ELSE FALSE END"
			)
	)
	private String answer;

	public UserPreferenceAnswer(String question, String answer) {
		this.question = requireText(question, "question");
		this.answer = requireText(answer, "answer");
	}

	private static String requireText(String value, String field) {
		if (requireNonNull(value, field + " must not be null").isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}

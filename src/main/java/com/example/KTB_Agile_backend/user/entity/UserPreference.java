package com.example.KTB_Agile_backend.user.entity;

import com.example.KTB_Agile_backend.common.entity.UpdatableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table( name = "user_preferences", uniqueConstraints = @UniqueConstraint(
				name = "uk_user_preference_user",
				columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference extends UpdatableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_preference_id", nullable = false)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
				name = "user_preference_answers",
				joinColumns = @JoinColumn(name = "user_preference_id")
	)

	@OrderColumn(name = "answer_order")
	@Getter(AccessLevel.NONE)
	private List<UserPreferenceAnswer> answers = new ArrayList<>();

	public UserPreference(User user, List<UserPreferenceAnswer> answers) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.answers.addAll(requireAnswers(answers));
	}

	public List<UserPreferenceAnswer> getAnswers() {
		return List.copyOf(answers);
	}

	public void replaceAnswers(List<UserPreferenceAnswer> answers) {
		this.answers.clear();
		this.answers.addAll(requireAnswers(answers));
	}

	private static List<UserPreferenceAnswer> requireAnswers(List<UserPreferenceAnswer> answers) {
		Objects.requireNonNull(answers, "answers must not be null");
		if (answers.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("answers must not contain null");
		}
		return answers;
	}
}

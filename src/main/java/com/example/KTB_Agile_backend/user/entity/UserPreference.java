package com.example.KTB_Agile_backend.user.entity;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
		name = "user_preferences",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_user_preference_user",
				columnNames = "user_id"
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

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

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public UserPreference(User user, List<UserPreferenceAnswer> answers) {
		this.user = user;
		this.answers.addAll(answers);
	}

	public List<UserPreferenceAnswer> getAnswers() {
		return List.copyOf(answers);
	}
}

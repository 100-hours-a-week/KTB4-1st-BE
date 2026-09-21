package com.example.KTB_Agile_backend.user.entity;

import com.example.KTB_Agile_backend.common.entity.UpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(
		name = "social_accounts",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_social_account_provider_user",
				columnNames = {"provider", "provider_user_id"}
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends UpdatableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "social_account_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 20)
	private String provider;

	@Column(name = "provider_user_id", nullable = false, length = 255)
	private String providerUserId;

	@Column(name = "last_login_at", nullable = false)
	private LocalDateTime lastLoginAt;

	public SocialAccount(User user, String provider, String providerUserId) {
		this.user = requireNonNull(user, "user must not be null");
		this.provider = requireText(provider, "provider", 20);
		this.providerUserId = requireText(providerUserId, "providerUserId", 255);
		this.lastLoginAt = LocalDateTime.now();
	}

	public void recordLogin() {
		this.lastLoginAt = LocalDateTime.now();
	}

	private static String requireText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
		}
		return value;
	}
}

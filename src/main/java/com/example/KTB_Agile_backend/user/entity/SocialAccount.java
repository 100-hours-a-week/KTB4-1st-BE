package com.example.KTB_Agile_backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
public class SocialAccount {

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

	@CreationTimestamp
	@Column(name = "linked_at", nullable = false, updatable = false)
	private LocalDateTime linkedAt;

	@Column(name = "last_login_at", nullable = false)
	private LocalDateTime lastLoginAt;

	public SocialAccount(User user, String provider, String providerUserId) {
		this.user = user;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.lastLoginAt = LocalDateTime.now();
	}

	public void recordLogin() {
		this.lastLoginAt = LocalDateTime.now();
	}
}

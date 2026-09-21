package com.example.KTB_Agile_backend.auth.entity;

import com.example.KTB_Agile_backend.common.entity.UpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "oauth_states")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthState extends UpdatableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "oauth_state_id", nullable = false)
	private Long oauthStateId;

	@Column(name = "state_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
	private String stateHash;

	@Column(nullable = false, length = 20)
	private String provider;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "consumed_at")
	private LocalDateTime consumedAt;

	public OAuthState(String stateHash, String provider, LocalDateTime expiresAt) {
		this.stateHash = requireText(stateHash, "stateHash", 64);
		this.provider = requireText(provider, "provider", 20);
		this.expiresAt = requireNonNull(expiresAt, "expiresAt must not be null");
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

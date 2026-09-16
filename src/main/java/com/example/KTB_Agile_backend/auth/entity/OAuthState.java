package com.example.KTB_Agile_backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "oauth_states")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthState {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "oauth_state_id", nullable = false)
	private Long oauthStateId;

	@Column(name = "state_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
	private String stateHash;

	@Column(nullable = false, length = 20)
	private String provider;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	public OAuthState(String stateHash, String provider, Instant expiresAt) {
		this.stateHash = stateHash;
		this.provider = provider;
		this.expiresAt = expiresAt;
	}
}

package com.example.KTB_Agile_backend.ai.text.entity;

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
import java.util.Objects;

@Entity
@Table(name = "moderation_checks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModerationCheck extends UpdatableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "moderation_check_id", nullable = false)
	private Long id;

	@Column(name = "check_id_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
	private String checkIdHash;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "content_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
	private String contentHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "consumed_at")
	private LocalDateTime consumedAt;

	public ModerationCheck(String checkIdHash, Long userId, String contentHash, LocalDateTime expiresAt) {
		this.checkIdHash = Objects.requireNonNull(checkIdHash, "checkIdHash must not be null");
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.contentHash = Objects.requireNonNull(contentHash, "contentHash must not be null");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
	}
}

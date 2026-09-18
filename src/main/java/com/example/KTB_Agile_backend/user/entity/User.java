package com.example.KTB_Agile_backend.user.entity;

import com.example.KTB_Agile_backend.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id", nullable = false)
	private Long id;

	@Column(name = "profile_image_url", length = 100)
	private String profileImageUrl;

	@Column(nullable = false, length = 200)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "user_role", nullable = false, length = 20)
	private UserRole userRole = UserRole.USER;

	@Enumerated(EnumType.STRING)
	@Column(name = "user_status", nullable = false, length = 20)
	private UserStatus userStatus = UserStatus.ACTIVE;

	public User(String nickname) {
		this.nickname = requireText(nickname, "nickname", 200);
	}

	public User(String nickname, String profileImageUrl) {
		this(nickname);
		this.profileImageUrl = requireMaxLength(profileImageUrl, "profileImageUrl", 100);
	}

	public void withdraw(LocalDateTime withdrawnAt) {
		markDeleted(withdrawnAt);
		this.userStatus = UserStatus.WITHDRAWN;
	}

	private static String requireText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return requireMaxLength(value, field, maxLength);
	}

	private static String requireMaxLength(String value, String field, int maxLength) {
		if (value != null && value.length() > maxLength) {
			throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
		}
		return value;
	}
}

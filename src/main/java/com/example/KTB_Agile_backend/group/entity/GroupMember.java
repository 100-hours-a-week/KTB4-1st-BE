package com.example.KTB_Agile_backend.group.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(
		name = "group_members",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_group_member_group_user",
				columnNames = {"group_id", "user_id"}
		),
		indexes = @Index(
				name = "idx_group_member_user_status_id",
				columnList = "user_id, user_status, group_members_id"
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "group_members_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private Group group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "location_verified_at")
	private LocalDateTime locationVerifiedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "user_status", nullable = false, length = 20)
	private GroupMemberStatus status;

	@Column(name = "left_at")
	private LocalDateTime leftAt;

	public GroupMember(Group group, User user) {
		this.group = requireNonNull(group, "group must not be null");
		this.user = requireNonNull(user, "user must not be null");
		this.status = GroupMemberStatus.ACTIVE;
	}

	@SuppressWarnings("PMD.NullAssignment")
	public void join() {
		this.status = GroupMemberStatus.ACTIVE;
		this.leftAt = null;
	}

	public void leave(LocalDateTime leftAt) {
		this.leftAt = requireNonNull(leftAt, "leftAt must not be null");
		this.status = GroupMemberStatus.LEFT;
	}

	public boolean isActive() {
		return status == GroupMemberStatus.ACTIVE;
	}
}

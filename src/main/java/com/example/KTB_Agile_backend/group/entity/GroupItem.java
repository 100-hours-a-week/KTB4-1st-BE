package com.example.KTB_Agile_backend.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(
		name = "group_items",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_group_item_group_item",
				columnNames = {"group_id", "item_id"}
		),
		indexes = @Index(
				name = "idx_group_item_group_deleted_item",
				columnList = "group_id, deleted_at, item_id"
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "group_item_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private Group group;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public GroupItem(Group group, Long itemId) {
		this.group = requireNonNull(group, "group must not be null");
		this.itemId = requireNonNull(itemId, "itemId must not be null");
	}

	public void delete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
		}
	}

	public boolean isActive() {
		return deletedAt == null;
	}
}

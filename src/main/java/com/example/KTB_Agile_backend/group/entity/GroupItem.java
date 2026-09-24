package com.example.KTB_Agile_backend.group.entity;

import com.example.KTB_Agile_backend.common.entity.SoftDeletableEntity;
import com.example.KTB_Agile_backend.item.entity.Item;
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
public class GroupItem extends SoftDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "group_item_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private Group group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	public GroupItem(Group group, Item item) {
		this.group = requireNonNull(group, "group must not be null");
		this.item = requireNonNull(item, "item must not be null");
	}

	public void delete() {
		if (!isDeleted()) {
			markDeleted(LocalDateTime.now());
		}
	}

	public void restore() {
		clearDeletedAt();
	}

	public boolean isActive() {
		return !isDeleted();
	}
}

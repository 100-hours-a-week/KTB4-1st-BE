package com.example.KTB_Agile_backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends UpdatableEntity {

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected void markDeleted(LocalDateTime deletedAt) {
		if (deletedAt == null) {
			throw new IllegalArgumentException("deletedAt must not be null");
		}
		this.deletedAt = deletedAt;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}

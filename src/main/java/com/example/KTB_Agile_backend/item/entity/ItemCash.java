package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
		name = "item_cash",
		uniqueConstraints = @UniqueConstraint(name = "uk_item_cash_keyword", columnNames = "keyword")
)
public class ItemCash extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_cash_id", nullable = false)
	private Long id;

	@Column(nullable = false, length = 255)
	private String keyword;

	@Column(name = "unit_price", nullable = false)
	private Long unitPrice;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	public ItemCash(String keyword, Long unitPrice, LocalDateTime expiresAt) {
		this.keyword = requireNonNull(keyword, "keyword must not be null");
		this.unitPrice = requireNonNull(unitPrice, "unitPrice must not be null");
		this.expiresAt = requireNonNull(expiresAt, "expiresAt must not be null");
	}

	public void refresh(Long unitPrice, LocalDateTime expiresAt) {
		this.unitPrice = requireNonNull(unitPrice, "unitPrice must not be null");
		this.expiresAt = requireNonNull(expiresAt, "expiresAt must not be null");
	}
}

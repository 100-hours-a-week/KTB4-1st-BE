package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.common.entity.SoftDeletableEntity;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@Table(name = "items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends SoftDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private Integer quantity = 1;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_state", nullable = false, length = 20)
	private ItemState itemState = ItemState.AVAILABLE;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 2000)
	private String content;

	@Column(name = "min_unit_price", nullable = false)
	private Long minUnitPrice = 0L;

	@Column(name = "max_unit_price", nullable = false)
	private Long maxUnitPrice = 0L;

	@Column(name = "exchange_urgency_score", nullable = false, precision = 3, scale = 2)
	private BigDecimal exchangeUrgencyScore = new BigDecimal("0.50");

	@Column(name = "value_gap_tolerance_score", nullable = false, precision = 3, scale = 2)
	private BigDecimal valueGapToleranceScore = new BigDecimal("0.50");

	public Item(User user, String title, String content) {
		this.user = requireNonNull(user, "user must not be null");
		this.title = requireNonNull(title, "title must not be null");
		this.content = requireNonNull(content, "content must not be null");
	}

	public Item(
			User user,
			String title,
			String content,
			Integer quantity,
			ItemState itemState,
			BigDecimal exchangeUrgencyScore,
			BigDecimal valueGapToleranceScore
	) {
		this(user, title, content);
		if (quantity == null || quantity < 1) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		this.quantity = quantity;
		this.itemState = requireNonNull(itemState, "itemState must not be null");
		this.exchangeUrgencyScore = requireNonNull(
				exchangeUrgencyScore,
				"exchangeUrgencyScore must not be null"
		);
		this.valueGapToleranceScore = requireNonNull(
				valueGapToleranceScore,
				"valueGapToleranceScore must not be null"
		);
	}

	public void update(
			String title,
			String content,
			Integer quantity,
			ItemState itemState,
			BigDecimal exchangeUrgencyScore,
			BigDecimal valueGapToleranceScore
	) {
		if (quantity == null || quantity < 1) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		this.title = requireNonNull(title, "title must not be null");
		this.content = requireNonNull(content, "content must not be null");
		this.quantity = quantity;
		this.itemState = requireNonNull(itemState, "itemState must not be null");
		this.exchangeUrgencyScore = requireNonNull(
				exchangeUrgencyScore,
				"exchangeUrgencyScore must not be null"
		);
		this.valueGapToleranceScore = requireNonNull(
				valueGapToleranceScore,
				"valueGapToleranceScore must not be null"
		);
	}

	public void deductForCompletedExchange(int quantity) {
		if (quantity < 1) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		this.quantity = Math.max(0, this.quantity - quantity);
		if (this.quantity == 0) {
			this.itemState = ItemState.UNAVAILABLE;
		}
	}

}

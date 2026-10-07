package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.common.entity.SoftDeletableEntity;
import com.example.KTB_Agile_backend.image.entity.Image;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@Table(name = "items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends SoftDeletableEntity {
	private static final int MINIMUM_EXCHANGE_QUANTITY = 1;
	private static final int MINIMUM_QUANTITY = 0;
	private static final int MAXIMUM_QUANTITY = 99;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "thumbnail_image_id")
	private Image thumbnailImage;

	@Column(nullable = false)
	@Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
	@Max(value = MAXIMUM_QUANTITY, message = "수량은 99개 이하여야 합니다.")
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

	@Column(name = "unit_price", nullable = false)
	private Long unitPrice = 0L;

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
		validateQuantity(quantity);
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
		validateQuantity(quantity);
		this.title = requireNonNull(title, "title must not be null");
		this.content = requireNonNull(content, "content must not be null");
		this.quantity = quantity;
		this.itemState = requireNonNull(itemState, "itemState must not be null");
		this.exchangeUrgencyScore = requireNonNull(exchangeUrgencyScore, "exchangeUrgencyScore must not be null");
		this.valueGapToleranceScore = requireNonNull(valueGapToleranceScore, "valueGapToleranceScore must not be null");
	}

	public void deductForCompletedExchange(int quantity) {
		if (quantity < MINIMUM_EXCHANGE_QUANTITY) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		this.quantity = Math.max(0, this.quantity - quantity);
		if (this.quantity == 0) {
			this.itemState = ItemState.UNAVAILABLE;
		}
	}

	private static void validateQuantity(Integer quantity) {
		if (quantity == null || quantity < MINIMUM_QUANTITY || quantity > MAXIMUM_QUANTITY) {
			throw new IllegalArgumentException("quantity must be between 0 and 99");
		}
	}

	public void setUnitPrices(Long unitPrice, Long minUnitPrice, Long maxUnitPrice) {
		this.unitPrice = requireNonNull(unitPrice, "unitPrice must not be null");
		this.minUnitPrice = requireNonNull(minUnitPrice, "minUnitPrice must not be null");
		this.maxUnitPrice = requireNonNull(maxUnitPrice, "maxUnitPrice must not be null");
	}

	public void setThumbnailImage(Image thumbnailImage) {
		this.thumbnailImage = requireNonNull(thumbnailImage, "thumbnailImage must not be null");
	}

	public void delete() {
		if (!isDeleted()) {
			markDeleted(LocalDateTime.now());
		}
	}

}

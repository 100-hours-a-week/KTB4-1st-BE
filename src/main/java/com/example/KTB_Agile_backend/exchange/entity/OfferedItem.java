package com.example.KTB_Agile_backend.exchange.entity;

import com.example.KTB_Agile_backend.item.entity.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exchange_request_offered_items", uniqueConstraints =
		@UniqueConstraint(name = "uk_exchange_offered_request_item", columnNames = {"exchange_request_id", "item_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfferedItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "exchange_request_offered_item_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exchange_request_id", nullable = false)
	private ExchangeRequest exchangeRequest;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@Column(nullable = false)
	private Integer quantity;

	OfferedItem(ExchangeRequest exchangeRequest, Item item, Integer quantity) {
		this.exchangeRequest = exchangeRequest;
		this.item = item;
		this.quantity = quantity;
	}
}

package com.example.KTB_Agile_backend.exchange.entity;

import com.example.KTB_Agile_backend.common.entity.UpdatableEntity;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exchange_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRequest extends UpdatableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "exchange_request_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_id", nullable = false)
	private User requester;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@Column(name = "requested_quantity", nullable = false)
	private Integer requestedQuantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "requested_status", nullable = false, length = 20)
	private ExchangeRequestStatus requestedStatus = ExchangeRequestStatus.PENDING;

	@OneToMany(mappedBy = "exchangeRequest", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OfferedItem> offeredItems = new ArrayList<>();

	public ExchangeRequest(User requester, Item item, Integer requestedQuantity) {
		this.requester = requester;
		this.item = item;
		this.requestedQuantity = requestedQuantity;
	}

	public void addOfferedItem(Item item, Integer quantity) {
		offeredItems.add(new OfferedItem(this, item, quantity));
	}

	public void updateRequestedQuantity(Integer requestedQuantity) {
		this.requestedQuantity = requestedQuantity;
	}

	public void removeOfferedItem(OfferedItem offeredItem) {
		offeredItems.remove(offeredItem);
	}

	public void changeStatus(ExchangeRequestStatus status) {
		if (status == null || status == ExchangeRequestStatus.PENDING) {
			throw new IllegalArgumentException("terminal status required");
		}
		this.requestedStatus = status;
	}
}

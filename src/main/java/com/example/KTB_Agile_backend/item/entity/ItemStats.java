package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "item_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemStats extends BaseEntity {

	@Id
	@Column(name = "item_id", nullable = false)
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@Column(name = "view_count", nullable = false)
	private Long viewCount = 0L;

	@Column(name = "like_count", nullable = false)
	private Long likeCount = 0L;

	public ItemStats(Item item) {
		this.item = requireNonNull(item, "item must not be null");
	}
}

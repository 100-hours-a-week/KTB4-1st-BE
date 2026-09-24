package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "item_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemLike extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_like_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public ItemLike(Item item, User user) {
		this.item = requireNonNull(item, "item must not be null");
		this.user = requireNonNull(user, "user must not be null");
	}
}

package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
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
@Table(name = "images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id")
	private Item item;

	@Column(name = "report_id")
	private Long reportId;

	@Column(name = "inquiry_id")
	private Long inquiryId;

	@Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
	private String imageUrl;

	public Image(Item item, String imageUrl) {
		this.item = item;
		this.imageUrl = requireNonNull(imageUrl, "imageUrl must not be null");
	}
}

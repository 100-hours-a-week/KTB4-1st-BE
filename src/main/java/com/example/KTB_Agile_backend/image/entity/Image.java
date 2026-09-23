package com.example.KTB_Agile_backend.image.entity;

import com.example.KTB_Agile_backend.common.entity.BaseEntity;
import com.example.KTB_Agile_backend.item.entity.Item;
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
@Table(name = "images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id")
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_id")
	@Column(name = "report_id")
	private Long reportId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inquiry_id")
	@Column(name = "inquiry_id")
	private Long inquiryId;

	@Column(name = "object_key", length = 512, unique = true)
	private String objectKey;

	@Column(name = "image_url", columnDefinition = "TEXT")
	private String imageUrl;

	public Image(User owner, String imageUrl) {
		this.owner = requireNonNull(owner, "owner must not be null");
		this.imageUrl = requireNonNull(imageUrl, "imageUrl must not be null");
	}

	public static Image fromS3Object(User owner, String objectKey) {
		Image image = new Image();
		image.owner = requireNonNull(owner, "owner must not be null");
		image.objectKey = requireNonNull(objectKey, "objectKey must not be null");
		return image;
	}

	public void attachTo(Item item) {
		if (this.item != null) {
			throw new IllegalStateException("image is already attached to an item");
		}
		this.item = requireNonNull(item, "item must not be null");
	}

	@SuppressWarnings("PMD.NullAssignment")
	public void detach() {
		this.item = null;
	}
}

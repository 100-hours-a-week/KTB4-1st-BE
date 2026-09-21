package com.example.KTB_Agile_backend.group.entity;

import com.example.KTB_Agile_backend.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends SoftDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "group_id", nullable = false)
	private Long id;

	@Column(name = "group_name", nullable = false, length = 30)
	private String groupName;

	@Column(name = "active_group_name", length = 30, unique = true)
	private String activeGroupName;

	@Column(name = "road_address", nullable = false, length = 100)
	private String roadAddress;

	@Column(name = "group_longitude", nullable = false, precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "group_latitude", nullable = false, precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(name = "group_content", nullable = false, length = 300)
	private String groupContent;

	private Group(
			String groupName,
			String roadAddress,
			BigDecimal longitude,
			BigDecimal latitude,
			String groupContent
	) {
		this.groupName = requireText(groupName, "groupName", 30);
		this.activeGroupName = this.groupName;
		this.roadAddress = requireText(roadAddress, "roadAddress", 100);
		this.longitude = requireCoordinate(longitude, "longitude", -180, 180);
		this.latitude = requireCoordinate(latitude, "latitude", -90, 90);
		this.groupContent = requireValue(groupContent, "groupContent", 300);
	}

	public static Group create(
			String groupName,
			String roadAddress,
			BigDecimal longitude,
			BigDecimal latitude,
			String groupContent
	) {
		return new Group(groupName, roadAddress, longitude, latitude, groupContent);
	}

	@SuppressWarnings("PMD.NullAssignment")
	public void delete() {
		if (!isDeleted()) {
			this.activeGroupName = null;
			markDeleted(LocalDateTime.now());
		}
	}

	private static String requireText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return requireMaxLength(value, field, maxLength);
	}

	private static String requireValue(String value, String field, int maxLength) {
		if (value == null) {
			throw new IllegalArgumentException(field + " must not be null");
		}
		return requireMaxLength(value, field, maxLength);
	}

	private static String requireMaxLength(String value, String field, int maxLength) {
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
		}
		return value;
	}

	private static BigDecimal requireCoordinate(
			BigDecimal value,
			String field,
			int min,
			int max
	) {
		if (value == null
				|| value.compareTo(BigDecimal.valueOf(min)) < 0
				|| value.compareTo(BigDecimal.valueOf(max)) > 0) {
			throw new IllegalArgumentException(field + " is out of range");
		}
		return value;
	}
}

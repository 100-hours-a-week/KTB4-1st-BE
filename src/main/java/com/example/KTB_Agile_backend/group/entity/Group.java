package com.example.KTB_Agile_backend.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group {

	private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
	private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
	private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
	private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "group_id", nullable = false)
	private Long id;

	@Column(name = "group_name", nullable = false, length = 30)
	private String groupName;

	@Column(name = "road_address", nullable = false, length = 100)
	private String roadAddress;

	@Column(name = "group_longitude", nullable = false, precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "group_latitude", nullable = false, precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(name = "group_content", nullable = false, length = 300)
	private String groupContent;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private Group(
			String groupName,
			String roadAddress,
			BigDecimal longitude,
			BigDecimal latitude,
			String groupContent
	) {
		this.groupName = requiredText(groupName, "groupName", 30);
		this.roadAddress = requiredText(roadAddress, "roadAddress", 100);
		this.longitude = coordinate(longitude, "longitude", MIN_LONGITUDE, MAX_LONGITUDE);
		this.latitude = coordinate(latitude, "latitude", MIN_LATITUDE, MAX_LATITUDE);
		this.groupContent = optionalText(groupContent, 300);
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

	public void delete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
		}
	}

	private static String requiredText(String value, String fieldName, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}

		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
		}
		return normalized;
	}

	private static String optionalText(String value, int maxLength) {
		if (value == null) {
			return "";
		}

		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException("groupContent must be at most " + maxLength + " characters");
		}
		return normalized;
	}

	private static BigDecimal coordinate(
			BigDecimal value,
			String fieldName,
			BigDecimal minimum,
			BigDecimal maximum
	) {
		BigDecimal coordinate = requireNonNull(value, fieldName + " must not be null");
		if (coordinate.scale() > 6) {
			throw new IllegalArgumentException(fieldName + " must have at most 6 decimal places");
		}
		if (coordinate.compareTo(minimum) < 0 || coordinate.compareTo(maximum) > 0) {
			throw new IllegalArgumentException(fieldName + " is out of range");
		}
		return coordinate;
	}
}

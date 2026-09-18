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
		this.groupName = groupName;
		this.roadAddress = roadAddress;
		this.longitude = longitude;
		this.latitude = latitude;
		this.groupContent = groupContent;
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
		if (!isDeleted()) {
			markDeleted(LocalDateTime.now());
		}
	}
}

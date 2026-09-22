package com.example.KTB_Agile_backend.group.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateGroupRequest(
		@NotBlank(message = "그룹명은 필수 입력값입니다.")
		@Size(max = 30, message = "그룹명은 30자 이내여야 합니다.")
		String groupName,

		@NotBlank(message = "도로명 주소는 필수 입력값입니다.")
		@Size(max = 100, message = "도로명 주소는 100자 이내여야 합니다.")
		String roadAddress,

		@NotNull(message = "경도는 필수 입력값입니다.")
		@DecimalMin(value = "-180", message = "경도 범위가 올바르지 않습니다.")
		@DecimalMax(value = "180", message = "경도 범위가 올바르지 않습니다.")
		@Digits(integer = 3, fraction = 6, message = "경도는 소수점 이하 6자리까지 입력할 수 있습니다.")
		BigDecimal longitude,

		@NotNull(message = "위도는 필수 입력값입니다.")
		@DecimalMin(value = "-90", message = "위도 범위가 올바르지 않습니다.")
		@DecimalMax(value = "90", message = "위도 범위가 올바르지 않습니다.")
		@Digits(integer = 2, fraction = 6, message = "위도는 소수점 이하 6자리까지 입력할 수 있습니다.")
		BigDecimal latitude,

		@Size(max = 300, message = "그룹 설명은 300자 이내여야 합니다.")
		String groupContent
) {

	public CreateGroupRequest {
		groupName = strip(groupName);
		roadAddress = strip(roadAddress);
		groupContent = groupContent == null ? "" : groupContent.strip();
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}

package com.example.KTB_Agile_backend.item.dto.request;

import com.example.KTB_Agile_backend.item.entity.ItemState;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.math.BigDecimal;
import java.util.List;

public record CreateItemRequest(
		@NotBlank(message = "제목은 필수 입력값입니다.")
		@Size(max = 100, message = "제목은 100자 이내여야 합니다.")
		String title,
		@NotBlank(message = "내용은 필수 입력값입니다.")
		@Size(max = 2000, message = "내용은 2000자 이내여야 합니다.")
		String content,
		@NotNull(message = "수량은 필수 입력값입니다.")
		@Positive(message = "수량은 1 이상이어야 합니다.")
		Integer quantity,
		@NotNull(message = "물품 상태는 필수 입력값입니다.")
		ItemState itemState,
		@NotNull(message = "거래 성향 점수는 필수 입력값입니다.")
		@DecimalMin(value = "0.00", message = "거래 성향 점수는 0 이상이어야 합니다.")
		@DecimalMax(value = "1.00", message = "거래 성향 점수는 1 이하여야 합니다.")
		@Digits(integer = 1, fraction = 2, message = "거래 성향 점수는 소수점 둘째 자리까지 입력할 수 있습니다.")
		BigDecimal exchangeUrgencyScore,
		@NotNull(message = "가치 차이 허용 점수는 필수 입력값입니다.")
		@DecimalMin(value = "0.00", message = "가치 차이 허용 점수는 0 이상이어야 합니다.")
		@DecimalMax(value = "1.00", message = "가치 차이 허용 점수는 1 이하여야 합니다.")
		@Digits(integer = 1, fraction = 2, message = "가치 차이 허용 점수는 소수점 둘째 자리까지 입력할 수 있습니다.")
		BigDecimal valueGapToleranceScore,
		@NotNull(message = "등록할 그룹은 필수 입력값입니다.")
		@Size(min = 1, message = "등록할 그룹을 하나 이상 선택해야 합니다.")
		@UniqueElements(message = "그룹 ID는 중복될 수 없습니다.")
		List<@NotNull(message = "그룹 ID는 필수 입력값입니다.") @Positive(message = "그룹 ID는 양수여야 합니다.") Long> groupIds,
		@NotNull(message = "이미지는 필수 입력값입니다.")
		@Size(min = 1, message = "이미지를 하나 이상 등록해야 합니다.")
		@UniqueElements(message = "이미지 ID는 중복될 수 없습니다.")
		List<@NotNull(message = "이미지 ID는 필수 입력값입니다.") @Positive(message = "이미지 ID는 양수여야 합니다.") Long> imageIds
) {

	public CreateItemRequest {
		title = strip(title);
		content = strip(content);
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}

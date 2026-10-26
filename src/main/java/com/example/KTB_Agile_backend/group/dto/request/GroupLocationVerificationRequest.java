package com.example.KTB_Agile_backend.group.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GroupLocationVerificationRequest(
		@NotNull(message = "위도는 필수 입력값입니다.")
		@DecimalMin(value = "-90", message = "위도 범위가 올바르지 않습니다.")
		@DecimalMax(value = "90", message = "위도 범위가 올바르지 않습니다.")
		BigDecimal latitude,

		@NotNull(message = "경도는 필수 입력값입니다.")
		@DecimalMin(value = "-180", message = "경도 범위가 올바르지 않습니다.")
		@DecimalMax(value = "180", message = "경도 범위가 올바르지 않습니다.")
		BigDecimal longitude,

		@NotNull(message = "정확도는 필수 입력값입니다.")
		@DecimalMin(value = "0", message = "정확도 값이 올바르지 않습니다.")
		BigDecimal accuracy
) {
}

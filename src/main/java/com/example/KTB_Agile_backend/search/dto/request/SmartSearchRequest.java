package com.example.KTB_Agile_backend.search.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record SmartSearchRequest(
		@NotNull(message = "그룹 ID는 필수 입력값입니다.")
		@Positive(message = "그룹 ID는 양수여야 합니다.")
		Long groupId,

		@NotEmpty(message = "교환할 물품을 하나 이상 선택해야 합니다.")
		List<@NotNull(message = "물품은 필수 입력값입니다.") @Valid SelectedItem> items,

		String preferenceText
) {
	public SmartSearchRequest {
		preferenceText = preferenceText == null ? "" : preferenceText.strip();
	}

	public record SelectedItem(
			@NotNull(message = "물품 ID는 필수 입력값입니다.")
			@Positive(message = "물품 ID는 양수여야 합니다.")
			Long itemId,

			@NotNull(message = "수량은 필수 입력값입니다.")
			@Positive(message = "수량은 양수여야 합니다.")
			Integer quantity
	) {
	}
}

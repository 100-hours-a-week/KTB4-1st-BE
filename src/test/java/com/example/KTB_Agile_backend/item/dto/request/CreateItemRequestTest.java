package com.example.KTB_Agile_backend.item.dto.request;

import com.example.KTB_Agile_backend.item.entity.ItemState;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateItemRequestTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void acceptsItemCreationPayload() {
		CreateItemRequest request = new CreateItemRequest(
				"임시 제목입니다.",
				"임시 내용입니다.",
				3,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				List.of(101L, 205L),
				List.of(1001L, 1002L)
		);

		assertThat(request.itemState()).isEqualTo(ItemState.AVAILABLE);
		assertThat(request.quantity()).isEqualTo(3);
		assertThat(request.exchangeUrgencyScore()).isEqualByComparingTo("0.50");
		assertThat(request.groupIds()).containsExactly(101L, 205L);
		assertThat(request.imageIds()).containsExactly(1001L, 1002L);
		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void rejectsInvalidItemCreationValues() {
		CreateItemRequest request = new CreateItemRequest(
				" ",
				"내용",
				0,
				ItemState.AVAILABLE,
				new BigDecimal("1.001"),
				new BigDecimal("0.30"),
				List.of(),
				List.of(0L)
		);

		assertThat(validator.validate(request)).isNotEmpty();
	}

	@Test
	void rejectsDuplicateGroupAndImageIds() {
		CreateItemRequest request = new CreateItemRequest(
				"제목",
				"내용",
				1,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				List.of(101L, 101L),
				List.of(1001L, 1001L)
		);

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getMessage())
				.contains("그룹 ID는 중복될 수 없습니다.", "이미지 ID는 중복될 수 없습니다.");
	}
}

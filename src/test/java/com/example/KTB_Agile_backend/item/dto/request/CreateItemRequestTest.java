package com.example.KTB_Agile_backend.item.dto.request;

import com.example.KTB_Agile_backend.item.entity.ItemState;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateItemRequestTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void acceptsItemCreationPayload() {
		CreateItemRequest request = new CreateItemRequest(
				"임시 제목입니다.",
				"임시 내용입니다.",
				"test-check-id",
				3,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				List.of(101L, 205L),
				List.of("images/42/1001.jpg", "images/42/1002.jpg"),
				null,
				null
		);

		assertThat(request.itemState()).isEqualTo(ItemState.AVAILABLE);
		assertThat(request.quantity()).isEqualTo(3);
		assertThat(request.exchangeUrgencyScore()).isEqualByComparingTo("0.50");
		assertThat(request.groupIds()).containsExactly(101L, 205L);
		assertThat(request.objectKeys()).containsExactly("images/42/1001.jpg", "images/42/1002.jpg");
		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void rejectsInvalidItemCreationValues() {
		CreateItemRequest request = new CreateItemRequest(
				" ",
				"내용",
				"test-check-id",
				0,
				ItemState.AVAILABLE,
				new BigDecimal("1.001"),
				new BigDecimal("0.30"),
				List.of(),
				List.of(" "),
				null,
				null
		);

		assertThat(validator.validate(request)).isNotEmpty();
	}

	@Test
	void rejectsDuplicateGroupAndImageKeys() {
		CreateItemRequest request = new CreateItemRequest(
				"제목",
				"내용",
				"test-check-id",
				1,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				List.of(101L, 101L),
				List.of("images/42/1001.jpg", "images/42/1001.jpg"),
				null,
				null
		);

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getMessage())
				.contains("그룹 ID는 중복될 수 없습니다.", "이미지 objectKey는 중복될 수 없습니다.");
	}

	@Test
	void validatesDetailThumbnailEntries() {
		var request = request(1, List.of(new CreateItemRequest.ImageDetailThumbnail(" ", "x".repeat(513))));

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getMessage())
				.contains("원본 objectKey는 필수 입력값입니다.", "상세용 썸네일 objectKey는 512자 이하여야 합니다.");

		assertThat(validator.validate(request(1, Collections.singletonList(null))))
				.extracting(violation -> violation.getMessage())
				.contains("상세용 썸네일 연결 항목은 필수입니다.");
	}

	@Test
	void acceptsOnlyQuantitiesFromZeroThroughNinetyNine() {
		assertThat(validator.validate(request(0))).isEmpty();
		assertThat(validator.validate(request(99))).isEmpty();
		assertThat(validator.validate(request(-1)))
				.extracting(violation -> violation.getMessage())
				.contains("수량은 0 이상이어야 합니다.");
		assertThat(validator.validate(request(100)))
				.extracting(violation -> violation.getMessage())
				.contains("수량은 99개 이하여야 합니다.");
	}

	@Test
	void rejectsUpdateQuantityAboveNinetyNine() {
		UpdateItemRequest request = new UpdateItemRequest(
				"제목",
				"내용",
				100,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				List.of(101L),
				List.of(1001L),
				null
		);

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getMessage())
				.contains("수량은 99개 이하여야 합니다.");
	}

	@Test
	void rejectsNegativeUpdateQuantity() {
		UpdateItemRequest request = new UpdateItemRequest(
				"제목",
				"내용",
				-1,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				List.of(101L),
				List.of(1001L),
				null
		);

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getMessage())
				.contains("수량은 0 이상이어야 합니다.");
	}

	private static CreateItemRequest request(int quantity) {
		return request(quantity, null);
	}

	private static CreateItemRequest request(
			int quantity, List<CreateItemRequest.ImageDetailThumbnail> imageDetailThumbnails) {
		return new CreateItemRequest(
				"제목",
				"내용",
				"test-check-id",
				quantity,
				ItemState.AVAILABLE,
				new BigDecimal("0.50"),
				new BigDecimal("0.30"),
				List.of(101L),
				List.of("images/42/1001.jpg"),
				null,
				imageDetailThumbnails
		);
	}
}

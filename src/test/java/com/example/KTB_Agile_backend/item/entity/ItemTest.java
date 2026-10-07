package com.example.KTB_Agile_backend.item.entity;

import com.example.KTB_Agile_backend.user.entity.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ItemTest {
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void enforcesQuantityRangeOnCreationAndUpdate() {
		User owner = mock(User.class);
		for (int quantity : List.of(0, 99)) {
			assertThat(item(owner, quantity).getQuantity()).isEqualTo(quantity);
			Item item = item(owner, 1);
			item.update("제목", "내용", quantity, ItemState.AVAILABLE, score(), score());
			assertThat(item.getQuantity()).isEqualTo(quantity);
		}

		for (int quantity : List.of(-1, 100)) {
			assertThatThrownBy(() -> item(owner, quantity))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("quantity must be between 0 and 99");
			Item item = item(owner, 1);
			assertThatThrownBy(() -> item.update("제목", "내용", quantity, ItemState.AVAILABLE, score(), score()))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("quantity must be between 0 and 99");
		}
	}

	@Test
	void allowsZeroQuantityAfterExchangeDepletesStock() {
		Item item = item(mock(User.class), 1);

		item.deductForCompletedExchange(1);

		assertThat(item.getQuantity()).isZero();
		assertThat(item.getItemState()).isEqualTo(ItemState.UNAVAILABLE);
		assertThat(validator.validate(item)).isEmpty();
	}

	private static Item item(User owner, int quantity) {
		return new Item(owner, "제목", "내용", quantity, ItemState.AVAILABLE, score(), score());
	}

	private static BigDecimal score() {
		return new BigDecimal("0.50");
	}
}

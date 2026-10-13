package com.example.KTB_Agile_backend.search.service;

import com.example.KTB_Agile_backend.item.entity.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartSearchBudgetCalculatorTest {

	@Test
	void totalsSelectedPricesAndCalculatesUnitPriceCeilingWithoutRounding() {
		Item first = item(10L, 100L);
		Item second = item(20L, 101L);
		SmartSearchBudgetCalculator calculator = new SmartSearchBudgetCalculator();

		BigDecimal budget = calculator.calculateBudget(List.of(first, second), Map.of(10L, 2, 20L, 1));

		assertThat(budget).isEqualByComparingTo("301");
		BigDecimal maximumUnitPrice = calculator.calculateMaximumUnitPrice(budget);
		assertThat(maximumUnitPrice).isEqualByComparingTo("361.20");
		assertThat(BigDecimal.valueOf(361).compareTo(maximumUnitPrice)).isLessThanOrEqualTo(0);
		assertThat(BigDecimal.valueOf(362).compareTo(maximumUnitPrice)).isGreaterThan(0);
	}

	private static Item item(Long id, Long unitPrice) {
		Item item = mock(Item.class);
		when(item.getId()).thenReturn(id);
		when(item.getUnitPrice()).thenReturn(unitPrice);
		return item;
	}
}

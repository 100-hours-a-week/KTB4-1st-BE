package com.example.KTB_Agile_backend.search.service;

import com.example.KTB_Agile_backend.item.entity.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class SmartSearchBudgetCalculator {
	private static final BigDecimal MAX_UNIT_PRICE_MULTIPLIER = new BigDecimal("1.20");

	public BigDecimal calculateBudget(List<Item> selectedItems, Map<Long, Integer> quantities) {
		return selectedItems.stream()
				.map(item -> BigDecimal.valueOf(item.getUnitPrice())
						.multiply(BigDecimal.valueOf(quantities.get(item.getId()))))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal calculateMaximumUnitPrice(BigDecimal budget) {
		return budget.multiply(MAX_UNIT_PRICE_MULTIPLIER);
	}
}

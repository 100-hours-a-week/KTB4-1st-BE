package com.example.KTB_Agile_backend.item.price.application;

import com.example.KTB_Agile_backend.item.price.ItemPriceRangeCalculator;
import com.example.KTB_Agile_backend.item.price.cache.ItemPriceCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemPriceLookupService {

	private final ItemPriceCache itemPriceCache;
	private final ItemPriceRangeCalculator itemPriceRangeCalculator;

	public Optional<ItemPriceRangeCalculator.PriceRange> cachedPrice(
			String keyword,
			BigDecimal valueGapToleranceScore,
			BigDecimal exchangeUrgencyScore
	) {
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
		if (normalizedKeyword != null) {
			var cachedUnitPrice = itemPriceCache.get(normalizedKeyword);
			if (cachedUnitPrice.isPresent()) {
				return Optional.of(itemPriceRangeCalculator.calculate(
						cachedUnitPrice.get(), valueGapToleranceScore, exchangeUrgencyScore));
			}
		}
		return Optional.empty();
	}
}

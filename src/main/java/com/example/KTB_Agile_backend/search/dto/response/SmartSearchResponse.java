package com.example.KTB_Agile_backend.search.dto.response;

import com.example.KTB_Agile_backend.item.dto.response.ItemSummary;

import java.util.List;

public record SmartSearchResponse(List<ItemSummary> items) {
	public SmartSearchResponse {
		items = List.copyOf(items);
	}
}

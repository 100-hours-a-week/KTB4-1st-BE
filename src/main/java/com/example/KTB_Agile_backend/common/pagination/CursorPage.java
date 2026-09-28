package com.example.KTB_Agile_backend.common.pagination;

import java.util.List;
import java.util.function.Function;

public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

	public CursorPage {
		items = List.copyOf(items);
	}

	public static <T> CursorPage<T> from(
			List<T> fetched,
			int size,
			Function<T, String> cursorEncoder
	) {
		boolean hasNext = fetched.size() > size;
		List<T> items = hasNext ? fetched.subList(0, size) : fetched;
		String nextCursor = hasNext ? cursorEncoder.apply(items.get(items.size() - 1)) : null;
		return new CursorPage<>(items, nextCursor, hasNext);
	}

	public static <T> CursorPage<T> fromIds(
			List<T> fetched,
			int size,
			Function<T, Long> idExtractor
	) {
		return from(fetched, size, item -> CursorCodec.encodeId(idExtractor.apply(item)));
	}
}

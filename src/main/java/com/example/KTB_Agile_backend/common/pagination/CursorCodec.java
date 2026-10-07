package com.example.KTB_Agile_backend.common.pagination;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;

public final class CursorCodec {
	private static final int COMPOSITE_PART_COUNT = 2;
	private static final long MIN_CURSOR_ID = 1L;
	private static final long MIN_COUNT = 0L;
	private static final String TIME_ID_SEPARATOR_REGEX = "\\|";
	private static final String COUNT_ID_SEPARATOR_REGEX = ":";

	private CursorCodec() {
	}

	public static Long decodeId(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}

		try {
			long id = Long.parseLong(decodeValue(cursor));
			if (id <= 0) {
				throw new IllegalArgumentException();
			}
			return id;
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_CURSOR, List.of(), exception);
		}
	}

	public static String encodeId(Long id) {
		return encodeValue(String.valueOf(id));
	}

	public static TimeIdCursor decodeTimeId(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			String[] parts = decodeParts(cursor, TIME_ID_SEPARATOR_REGEX);
			long id = Long.parseLong(parts[1]);
			if (id < MIN_CURSOR_ID) {
				throw new IllegalArgumentException();
			}
			return new TimeIdCursor(LocalDateTime.parse(parts[0]), id);
		} catch (IllegalArgumentException | DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_CURSOR, List.of(), exception);
		}
	}

	public static String encodeTimeId(LocalDateTime time, Long id) {
		return encodeValue(time + "|" + id);
	}

	public static CountIdCursor decodeCountId(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			String[] parts = decodeParts(cursor, COUNT_ID_SEPARATOR_REGEX);
			long count = Long.parseLong(parts[0]);
			long id = Long.parseLong(parts[1]);
			if (count < MIN_COUNT || id < MIN_CURSOR_ID) {
				throw new IllegalArgumentException();
			}
			return new CountIdCursor(count, id);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_CURSOR, List.of(), exception);
		}
	}

	public static String encodeCountId(Long count, Long id) {
		return encodeValue(count + ":" + id);
	}

	private static String[] decodeParts(String cursor, String separatorRegex) {
		String[] parts = decodeValue(cursor).split(separatorRegex, -1);
		if (parts.length != COMPOSITE_PART_COUNT) {
			throw new IllegalArgumentException();
		}
		return parts;
	}

	public static String encodeValue(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	public static String decodeValue(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
	}

	public record TimeIdCursor(LocalDateTime time, Long id) {
	}

	public record CountIdCursor(Long count, Long id) {
	}
}

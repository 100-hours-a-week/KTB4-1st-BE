package com.example.KTB_Agile_backend.common.pagination;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public final class CursorCodec {

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

	public static String encodeValue(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	public static String decodeValue(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
	}
}

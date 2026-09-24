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
			long id = Long.parseLong(new String(
					Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
			if (id <= 0) {
				throw new IllegalArgumentException();
			}
			return id;
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.BAD_REQUEST, "cursor가 올바르지 않습니다.", List.of(), exception);
		}
	}

	public static String encodeId(Long id) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
				String.valueOf(id).getBytes(StandardCharsets.UTF_8));
	}
}

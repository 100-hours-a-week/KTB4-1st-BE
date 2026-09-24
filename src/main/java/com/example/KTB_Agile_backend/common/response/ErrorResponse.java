package com.example.KTB_Agile_backend.common.response;

import java.util.List;

public record ErrorResponse(String code, String message, List<Field> details) {

	public record Field(String field, String reason) {
	}
}

package com.example.KTB_Agile_backend.common.response;

public record ApiResponse<T>(T data, ErrorResponse error) {
}

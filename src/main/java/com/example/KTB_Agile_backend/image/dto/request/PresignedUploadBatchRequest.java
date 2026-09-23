package com.example.KTB_Agile_backend.image.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PresignedUploadBatchRequest(
		@NotEmpty(message = "이미지는 1장 이상이어야 합니다.")
		@Size(max = 3, message = "이미지는 최대 3장까지 업로드할 수 있습니다.")
		List<@NotNull @Valid PresignedUploadRequest> images
) {
}

package com.example.KTB_Agile_backend.group.dto.response;

public record GroupLocationVerificationResponse(
		boolean verified,
		FailureReason failureReason
) {

	public enum FailureReason {
		ACCURACY_TOO_LOW,
		DISTANCE_TOO_FAR
	}
}

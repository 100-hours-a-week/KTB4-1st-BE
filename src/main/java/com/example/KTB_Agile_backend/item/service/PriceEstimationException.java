package com.example.KTB_Agile_backend.item.service;

public class PriceEstimationException extends RuntimeException {
	private final PriceEstimationRetryPolicy.Failure failure;

	public PriceEstimationException(PriceEstimationRetryPolicy.Failure failure, Throwable cause) {
		super("Item price estimation failed: " + failure, cause);
		this.failure = failure;
	}

	public PriceEstimationException(PriceEstimationRetryPolicy.Failure failure) {
		this(failure, null);
	}

	public PriceEstimationRetryPolicy.Failure failure() {
		return failure;
	}
}

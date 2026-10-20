package com.example.KTB_Agile_backend.item.service;

public class PriceEstimationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final PriceEstimationRetryPolicy.Failure failureType;

	public PriceEstimationException(PriceEstimationRetryPolicy.Failure failure, Throwable cause) {
		super("Item price estimation failed: " + failure, cause);
		this.failureType = failure;
	}

	public PriceEstimationException(PriceEstimationRetryPolicy.Failure failure) {
		this(failure, null);
	}

	public PriceEstimationRetryPolicy.Failure failure() {
		return failureType;
	}
}

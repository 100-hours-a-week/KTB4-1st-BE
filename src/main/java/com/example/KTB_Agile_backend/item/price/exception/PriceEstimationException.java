package com.example.KTB_Agile_backend.item.price.exception;

import com.example.KTB_Agile_backend.item.price.PriceEstimationFailure;

public class PriceEstimationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final PriceEstimationFailure failureType;

	public PriceEstimationException(PriceEstimationFailure failure, Throwable cause) {
		super("Item price estimation failed: " + failure, cause);
		this.failureType = failure;
	}

	public PriceEstimationException(PriceEstimationFailure failure) {
		this(failure, null);
	}

	public PriceEstimationFailure failure() {
		return failureType;
	}
}

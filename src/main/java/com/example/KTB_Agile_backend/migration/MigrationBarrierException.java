package com.example.KTB_Agile_backend.migration;

public class MigrationBarrierException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MigrationBarrierException(String message) {
		super(message);
	}

	public MigrationBarrierException(String message, Throwable cause) {
		super(message, cause);
	}
}

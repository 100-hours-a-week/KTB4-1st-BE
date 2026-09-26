package com.example.KTB_Agile_backend.common.exception;

import org.springframework.http.HttpStatus;

public interface ApiErrorCode {

	String value();

	HttpStatus status();

	String message();
}

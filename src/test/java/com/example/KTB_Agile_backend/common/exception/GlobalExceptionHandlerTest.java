package com.example.KTB_Agile_backend.common.exception;

import com.example.KTB_Agile_backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

	@Test
	void returnsTheSameUnexpectedErrorForEveryUri() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		ResponseEntity<ApiResponse<Void>> authResponse = handler.handleUnexpectedException(
				new RuntimeException("database failure"),
				request("/auth/refresh")
		);
		ResponseEntity<ApiResponse<Void>> groupResponse = handler.handleUnexpectedException(
				new RuntimeException("database failure"),
				request("/groups")
		);

		assertEquals(authResponse.getStatusCode(), groupResponse.getStatusCode());
		assertEquals(authResponse.getBody(), groupResponse.getBody());
		assertEquals("INTERNAL_SERVER_ERROR", authResponse.getBody().error().code());
		assertEquals("서버 오류가 발생했습니다.", authResponse.getBody().error().message());
	}

	private static HttpServletRequest request(String uri) {
		return new MockHttpServletRequest("GET", uri);
	}
}

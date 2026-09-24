package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.image.ai.ImageAiAnalysisService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageAiAnalysisServiceTest {

	@Test
	void sendsImageUrlArrayAndForwardsAiErrorBodyAndStatus() throws Exception {
		String errorBody = "{\"error\":\"invalid_input\",\"message\":\"분석할 수 없는 이미지입니다.\"}";
		AtomicReference<String> requestBody = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/analyze", exchange -> {
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] responseBody = errorBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
			exchange.sendResponseHeaders(400, responseBody.length);
			exchange.getResponseBody().write(responseBody);
			exchange.close();
		});
		server.start();

		try {
			S3ImageObjectService s3 = mock(S3ImageObjectService.class);
			List<String> objectKeys = List.of("images/42/first.jpg", "images/42/second.jpg");
			when(s3.presignedAnalysisUrl(objectKeys.get(0))).thenReturn("https://s3.example/first");
			when(s3.presignedAnalysisUrl(objectKeys.get(1))).thenReturn("https://s3.example/second");
			ImageAiAnalysisService service = new ImageAiAnalysisService(
					s3, "http://127.0.0.1:" + server.getAddress().getPort() + "/analyze", 2, 2);

			ResponseEntity<String> response = service.analyze(42L, objectKeys);

			assertThat(requestBody.get()).isEqualTo(
					"{\"imageUrls\":[\"https://s3.example/first\",\"https://s3.example/second\"]}");
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
			assertThat(response.getBody()).isEqualTo(errorBody);
			verify(s3).validatePendingObjects(42L, objectKeys);
		} finally {
			server.stop(0);
		}
	}
}

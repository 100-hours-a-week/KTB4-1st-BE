package com.example.KTB_Agile_backend.image.controller;

import com.example.KTB_Agile_backend.common.exception.GlobalExceptionHandler;
import com.example.KTB_Agile_backend.image.dto.response.PresignedUploadResponse;
import com.example.KTB_Agile_backend.image.service.ImageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

	private ImageUploadService imageUploadService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		imageUploadService = mock(ImageUploadService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new ImageController(imageUploadService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void issuesPresignedUrlForAuthenticatedUser() throws Exception {
		when(imageUploadService.issue(eq(42L), any()))
				.thenReturn(new PresignedUploadResponse(
						"https://s3.example/upload",
						"images/42/object.jpg",
						600
				));

		mockMvc.perform(post("/images/presigned-url")
					.principal(new UsernamePasswordAuthenticationToken("42", null))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"contentType":"image/jpeg"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.uploadUrl").value("https://s3.example/upload"))
				.andExpect(jsonPath("$.data.objectKey").value("images/42/object.jpg"))
				.andExpect(jsonPath("$.data.expiresInSeconds").value(600));

		verify(imageUploadService).issue(eq(42L), any());
	}
}

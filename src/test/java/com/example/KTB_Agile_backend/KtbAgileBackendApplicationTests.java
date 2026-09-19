package com.example.KTB_Agile_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KtbAgileBackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void allowsFrontendCorsPreflight() throws Exception {
		mockMvc.perform(options("/auth/refresh")
					.header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void returnsStandardUnauthorizedResponseWhenLogoutHasNoAccessToken() throws Exception {
		mockMvc.perform(post("/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
				.andExpect(jsonPath("$.error.message")
						.value("로그인이 필요합니다."))
				.andExpect(jsonPath("$.error.details").isEmpty());
	}

}

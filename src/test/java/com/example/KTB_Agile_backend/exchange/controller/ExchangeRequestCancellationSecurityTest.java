package com.example.KTB_Agile_backend.exchange.controller;

import com.example.KTB_Agile_backend.exchange.service.ExchangeRequestQueryService;
import com.example.KTB_Agile_backend.exchange.service.ExchangeRequestService;
import com.example.KTB_Agile_backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRequestController.class)
@Import(SecurityConfig.class)
class ExchangeRequestCancellationSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ExchangeRequestService service;

	@MockitoBean
	private ExchangeRequestQueryService queryService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private JpaMetamodelMappingContext jpaMappingContext;

	@Test
	void rejectsMissingInvalidAndExpiredTokensBeforeCallingService() throws Exception {
		when(jwtDecoder.decode("invalid")).thenThrow(new JwtException("invalid token"));
		when(jwtDecoder.decode("expired")).thenThrow(new JwtException("expired token"));
		for (String path : List.of("/exchange-requests/301", "/api/exchange-requests/301")) {
			for (String token : List.of("", "invalid", "expired")) {
				mockMvc.perform(delete(path).servletPath(path)
							.header("Authorization", "Bearer " + token))
						.andExpect(status().isUnauthorized())
						.andExpect(jsonPath("$.data").isEmpty())
						.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
						.andExpect(jsonPath("$.error.message")
								.value("로그인이 필요하거나 Access Token이 만료되었거나 유효하지 않습니다."))
						.andExpect(jsonPath("$.error.details").isEmpty());
			}
		}
		verifyNoInteractions(service, queryService);
	}
}

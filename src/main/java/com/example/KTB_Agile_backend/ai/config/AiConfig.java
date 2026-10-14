package com.example.KTB_Agile_backend.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

	@Bean
	RestClient aiRestClient(AiProperties aiProperties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(aiProperties.connectTimeoutSeconds()));
		requestFactory.setReadTimeout(Duration.ofSeconds(aiProperties.readTimeoutSeconds()));
		return RestClient.builder().requestFactory(requestFactory).build();
	}
}

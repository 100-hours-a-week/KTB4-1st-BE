package com.example.KTB_Agile_backend.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigTest {

	@Test
	void bindsAiPropertiesAndCreatesSharedRestClient() {
		new ApplicationContextRunner()
				.withUserConfiguration(AiConfig.class)
				.withPropertyValues(
						"ai.smart-search-url=https://ai.example/search",
						"ai.connect-timeout-seconds=2",
						"ai.read-timeout-seconds=10"
				)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(AiProperties.class).smartSearchUrl())
							.isEqualTo("https://ai.example/search");
					assertThat(context.getBean(AiProperties.class).connectTimeoutSeconds()).isEqualTo(2);
					assertThat(context.getBean(AiProperties.class).readTimeoutSeconds()).isEqualTo(10);
					assertThat(context).hasSingleBean(RestClient.class);
				});
	}
}

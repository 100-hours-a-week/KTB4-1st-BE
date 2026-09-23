package com.example.KTB_Agile_backend.image.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
public class S3Config {

	@Bean(destroyMethod = "close")
	S3Presigner s3Presigner(
			@Value("${cloud.aws.s3.region}") String region
	) {
		return S3Presigner.builder()
				.region(Region.of(region))
				.build();
	}
}

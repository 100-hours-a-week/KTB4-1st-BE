package com.example.KTB_Agile_backend.item.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PriceEstimationRetryProperties.class)
class PriceEstimationRetryConfig {
}

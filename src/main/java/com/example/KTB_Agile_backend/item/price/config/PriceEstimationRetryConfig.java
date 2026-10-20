package com.example.KTB_Agile_backend.item.price.config;

import com.example.KTB_Agile_backend.item.price.config.PriceEstimationRetryConfig;
import com.example.KTB_Agile_backend.item.price.config.PriceEstimationRetryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PriceEstimationRetryProperties.class)
class PriceEstimationRetryConfig {
}

package com.example.KTB_Agile_backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "viewCountExecutor")
	public ThreadPoolTaskExecutor viewCountExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("view-count-");
		// ponytail: Counts can be lost on queue overflow or shutdown; use a durable queue if that becomes unacceptable.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
		return executor;
	}
}

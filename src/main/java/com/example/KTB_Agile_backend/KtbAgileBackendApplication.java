package com.example.KTB_Agile_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KtbAgileBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(KtbAgileBackendApplication.class, args);
	}

}

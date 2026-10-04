package com.example.KTB_Agile_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod")
public class PrometheusSecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain prometheusSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/actuator/prometheus")
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}

	@Bean
	PasswordEncoder prometheusPasswordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsService prometheusUserDetailsService(
			@Value("${monitoring.prometheus.auth.username}") String username,
			@Value("${monitoring.prometheus.auth.password}") String password,
			PasswordEncoder prometheusPasswordEncoder
	) {
		if (username.isBlank() || password.isBlank()) {
			throw new IllegalArgumentException("Prometheus authentication credentials must not be blank");
		}
		return new InMemoryUserDetailsManager(User.withUsername(username)
				.password(prometheusPasswordEncoder.encode(password))
				.roles("PROMETHEUS")
				.build());
	}
}

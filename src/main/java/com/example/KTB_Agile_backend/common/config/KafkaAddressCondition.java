package com.example.KTB_Agile_backend.common.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public final class KafkaAddressCondition {

	private static final String ADDRESS_ENV = "KAFKA_BOOTSTRAP_SERVERS";

	private KafkaAddressCondition() {
	}

	private static boolean isConfigured(ConditionContext context) {
		return StringUtils.hasText(context.getEnvironment().getProperty(ADDRESS_ENV));
	}

	public static class Configured implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return isConfigured(context);
		}
	}

	public static class Missing implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !isConfigured(context);
		}
	}
}

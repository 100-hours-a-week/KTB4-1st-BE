package com.example.KTB_Agile_backend.migration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigrationTransactionBarrierAspect {

	private final MigrationBarrierManager barrierManager;

	public MigrationTransactionBarrierAspect(MigrationBarrierManager barrierManager) {
		this.barrierManager = barrierManager;
	}

	@Around("@annotation(org.springframework.transaction.annotation.Transactional) "
			+ "|| @within(org.springframework.transaction.annotation.Transactional)")
	public Object guardTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
		barrierManager.enterDatabaseOperation();
		try {
			return joinPoint.proceed();
		} finally {
			barrierManager.exitDatabaseOperation();
		}
	}
}

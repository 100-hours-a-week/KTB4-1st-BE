package com.example.KTB_Agile_backend.migration;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

final class MigrationRoutingDataSource extends AbstractRoutingDataSource {

	MigrationRoutingDataSource(DataSource ec2, DataSource rds, MigrationBarrierManager barrierManager) {
		setTargetDataSources(Map.of(
				MigrationTarget.EC2, ec2,
				MigrationTarget.RDS, rds
		));
		setDefaultTargetDataSource(ec2);
		setLenientFallback(false);
		this.barrierManager = barrierManager;
	}

	private final MigrationBarrierManager barrierManager;

	@Override
	protected Object determineCurrentLookupKey() {
		return barrierManager.currentTarget();
	}

	void switchTarget(MigrationTarget target) {
		// The lookup key is read for every newly acquired connection.
		barrierManager.setCurrentTarget(target);
	}
}

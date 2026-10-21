package com.example.KTB_Agile_backend.migration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true")
public class MigrationDataSourceConfig {

	@Bean(name = "migrationEc2DataSource", destroyMethod = "close")
	HikariDataSource migrationEc2DataSource(
			@Value("${migration.datasource.ec2.url}") String url,
			@Value("${migration.datasource.ec2.username}") String username,
			@Value("${migration.datasource.ec2.password}") String password,
			@Value("${migration.datasource.maximum-pool-size:20}") int maximumPoolSize,
			@Value("${migration.datasource.minimum-idle:2}") int minimumIdle,
			@Value("${migration.datasource.connection-timeout-ms:5000}") long connectionTimeout
	) {
		return createDataSource(
				"migration-ec2", url, username, password,
				maximumPoolSize, minimumIdle, connectionTimeout
		);
	}

	@Bean(name = "migrationRdsDataSource", destroyMethod = "close")
	HikariDataSource migrationRdsDataSource(
			@Value("${migration.datasource.rds.url}") String url,
			@Value("${migration.datasource.rds.username}") String username,
			@Value("${migration.datasource.rds.password}") String password,
			@Value("${migration.datasource.maximum-pool-size:20}") int maximumPoolSize,
			@Value("${migration.datasource.minimum-idle:2}") int minimumIdle,
			@Value("${migration.datasource.connection-timeout-ms:5000}") long connectionTimeout
	) {
		return createDataSource(
				"migration-rds", url, username, password,
				maximumPoolSize, minimumIdle, connectionTimeout
		);
	}

	@Bean
	MigrationRoutingDataSource migrationRoutingDataSource(
			@Qualifier("migrationEc2DataSource") HikariDataSource ec2DataSource,
			@Qualifier("migrationRdsDataSource") HikariDataSource rdsDataSource,
			MigrationBarrierManager barrierManager
	) {
		MigrationRoutingDataSource routingDataSource =
				new MigrationRoutingDataSource(ec2DataSource, rdsDataSource, barrierManager);
		barrierManager.attachRoutingDataSource(routingDataSource);
		return routingDataSource;
	}

	@Bean
	@Primary
	DataSource dataSource(MigrationRoutingDataSource routingDataSource) {
		return new LazyConnectionDataSourceProxy(routingDataSource);
	}

	private static HikariDataSource createDataSource(
			String poolName,
			String url,
			String username,
			String password,
			int maximumPoolSize,
			int minimumIdle,
			long connectionTimeout
	) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setPoolName(poolName);
		dataSource.setJdbcUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setMaximumPoolSize(maximumPoolSize);
		dataSource.setMinimumIdle(minimumIdle);
		dataSource.setConnectionTimeout(connectionTimeout);
		return dataSource;
	}
}

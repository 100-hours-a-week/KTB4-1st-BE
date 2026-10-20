package com.example.KTB_Agile_backend.migration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true")
public class MigrationBarrierManager {

	private enum BarrierState {
		OPEN,
		PREPARING
	}

	private final HikariDataSource ec2DataSource;
	private final HikariDataSource rdsDataSource;
	private final ReentrantReadWriteLock databaseLock = new ReentrantReadWriteLock(true);
	private final long timeoutMillis;
	private final AtomicReference<MigrationRoutingDataSource> routingDataSource = new AtomicReference<>();

	private BarrierState state = BarrierState.OPEN;
	private MigrationTarget currentTarget = MigrationTarget.EC2;
	private MigrationTarget preparedTarget;

	public MigrationBarrierManager(
			@Qualifier("migrationEc2DataSource")
			HikariDataSource ec2DataSource,
			@Qualifier("migrationRdsDataSource")
			HikariDataSource rdsDataSource,
			@Value("${migration.barrier.timeout-ms:30000}") long timeoutMillis
	) {
		this.ec2DataSource = ec2DataSource;
		this.rdsDataSource = rdsDataSource;
		this.timeoutMillis = timeoutMillis;
	}

	void attachRoutingDataSource(MigrationRoutingDataSource routingDataSource) {
		if (!this.routingDataSource.compareAndSet(null, routingDataSource)) {
			throw new IllegalStateException("migration routing datasource is already attached");
		}
	}

	public void enterDatabaseOperation() {
		try {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
			while (true) {
				synchronized (this) {
					while (state != BarrierState.OPEN) {
						long remaining = deadline - System.nanoTime();
						if (remaining <= 0) {
							throw new MigrationBarrierException("database migration barrier timeout");
						}
						TimeUnit.NANOSECONDS.timedWait(this, remaining);
					}
				}
				long remaining = deadline - System.nanoTime();
				if (remaining <= 0 ||
						!databaseLock.readLock().tryLock(remaining, TimeUnit.NANOSECONDS)) {
					throw new MigrationBarrierException("database migration barrier timeout");
				}
				synchronized (this) {
					if (state == BarrierState.OPEN) {
						return;
					}
				}
				databaseLock.readLock().unlock();
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new MigrationBarrierException("database migration barrier interrupted");
		}
	}

	public void exitDatabaseOperation() {
		databaseLock.readLock().unlock();
	}

	public synchronized MigrationTarget currentTarget() {
		return currentTarget;
	}

	synchronized void setCurrentTarget(MigrationTarget target) {
		currentTarget = target;
	}

	public void prepare(MigrationTarget target) {
		synchronized (this) {
			if (state == BarrierState.PREPARING) {
				if (preparedTarget != target) {
					throw new MigrationBarrierException("migration barrier is already preparing another target");
				}
				return;
			}
			if (currentTarget == target) {
				return;
			}
			state = BarrierState.PREPARING;
			preparedTarget = target;
		}
		try {
			if (!databaseLock.writeLock().tryLock(timeoutMillis, TimeUnit.MILLISECONDS)) {
				synchronized (this) {
					state = BarrierState.OPEN;
					preparedTarget = null;
					notifyAll();
				}
				throw new MigrationBarrierException("database migration barrier timeout");
			}
			databaseLock.writeLock().unlock();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			synchronized (this) {
				state = BarrierState.OPEN;
				preparedTarget = null;
				notifyAll();
			}
			throw new MigrationBarrierException("database migration barrier interrupted");
		}
	}

	public synchronized void commit(MigrationTarget target) {
		if (state == BarrierState.OPEN && currentTarget == target) {
			return;
		}
		if (state != BarrierState.PREPARING || preparedTarget != target) {
			throw new MigrationBarrierException("migration barrier is not prepared for " + target);
		}
		routingDataSource().switchTarget(target);
	}

	public void release(MigrationTarget target) {
		databaseLock.writeLock().lock();
		try {
			synchronized (this) {
				if (state == BarrierState.OPEN) {
					if (currentTarget != target) {
						throw new MigrationBarrierException("database is already routed to " + currentTarget);
					}
					return;
				}
				if (preparedTarget != target || currentTarget != target) {
					throw new MigrationBarrierException("migration barrier target does not match " + target);
				}
				state = BarrierState.OPEN;
				preparedTarget = null;
				notifyAll();
			}
		} finally {
			databaseLock.writeLock().unlock();
		}
	}

	public void abort(MigrationTarget target) {
		databaseLock.writeLock().lock();
		try {
			synchronized (this) {
				if (state == BarrierState.OPEN) {
					if (currentTarget != MigrationTarget.EC2) {
						throw new MigrationBarrierException("cannot abort after routing to " + currentTarget);
					}
					return;
				}
				if (preparedTarget != target) {
					throw new MigrationBarrierException("migration barrier target does not match " + target);
				}
				routingDataSource().switchTarget(MigrationTarget.EC2);
				state = BarrierState.OPEN;
				preparedTarget = null;
				notifyAll();
			}
		} finally {
			databaseLock.writeLock().unlock();
		}
	}

	public void probe(MigrationTarget target) {
		HikariDataSource dataSource = target == MigrationTarget.RDS ? rdsDataSource : ec2DataSource;
		try (var connection = dataSource.getConnection();
			 var statement = connection.createStatement();
			 var result = statement.executeQuery(
					 "SELECT 1, @@read_only, @@super_read_only")) {
			if (!result.next() || result.getInt(1) != 1) {
				throw new MigrationBarrierException(target + " health probe returned an invalid result");
			}
			if (target == MigrationTarget.RDS
					&& (result.getBoolean(2) || result.getBoolean(3))) {
				throw new MigrationBarrierException("RDS is not writable");
			}
		} catch (SQLException exception) {
			throw new MigrationBarrierException(target + " health probe failed: " + exception.getMessage());
		}
	}

	public synchronized boolean isPreparing() {
		return state == BarrierState.PREPARING;
	}

	private MigrationRoutingDataSource routingDataSource() {
		MigrationRoutingDataSource dataSource = routingDataSource.get();
		if (dataSource == null) {
			throw new IllegalStateException("migration routing datasource is not initialized");
		}
		return dataSource;
	}
}

package com.example.KTB_Agile_backend.migration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/admin/migration")
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true")
public class MigrationAdminController {

	private final MigrationBarrierManager barrierManager;

	public MigrationAdminController(MigrationBarrierManager barrierManager) {
		this.barrierManager = barrierManager;
	}

	@PostMapping("/prepare")
	public ResponseEntity<Map<String, String>> prepare(@RequestParam MigrationTarget target) {
		return execute(() -> barrierManager.prepare(target), "prepared");
	}

	@PostMapping("/commit")
	public ResponseEntity<Map<String, String>> commit(@RequestParam MigrationTarget target) {
		return execute(() -> barrierManager.commit(target), "committed");
	}

	@PostMapping("/health")
	public ResponseEntity<Map<String, String>> health(@RequestParam MigrationTarget target) {
		return execute(() -> {
			if (barrierManager.currentTarget() != target) {
				throw new MigrationBarrierException("database is routed to "
						+ barrierManager.currentTarget() + ", not " + target);
			}
			barrierManager.probe(target);
		}, "healthy");
	}

	@PostMapping("/release")
	public ResponseEntity<Map<String, String>> release(@RequestParam MigrationTarget target) {
		return execute(() -> barrierManager.release(target), "released");
	}

	@PostMapping("/abort")
	public ResponseEntity<Map<String, String>> abort(@RequestParam MigrationTarget target) {
		return execute(() -> barrierManager.abort(target), "aborted");
	}

	private static ResponseEntity<Map<String, String>> execute(
			Runnable action,
			String status
	) {
		try {
			action.run();
			return ResponseEntity.ok(Map.of("status", status));
		} catch (MigrationBarrierException exception) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("status", "blocked", "message", exception.getMessage()));
		}
	}
}

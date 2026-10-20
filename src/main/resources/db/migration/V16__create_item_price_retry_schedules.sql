CREATE TABLE item_price_estimation_retry_schedules (
    request_id VARCHAR(36) NOT NULL,
    original_request_event_id VARCHAR(36) NOT NULL,
    attempt_count INT NOT NULL,
    next_attempt_at DATETIME(6) NULL,
    validation_retry_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (request_id),
    KEY ix_price_retry_due (next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

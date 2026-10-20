ALTER TABLE items
    ADD COLUMN price_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN price_request_id VARCHAR(36) NULL;

UPDATE items SET price_status = 'SUCCESS';

CREATE TABLE item_price_estimation_outbox_events (
    outbox_event_id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    original_request_event_id VARCHAR(36) NOT NULL,
    deduplication_key VARCHAR(128) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    item_id BIGINT NOT NULL,
    attempt_count INT NOT NULL,
    payload LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (outbox_event_id),
    UNIQUE KEY uk_price_outbox_event_id (event_id),
    UNIQUE KEY uk_price_outbox_deduplication_key (deduplication_key),
    CONSTRAINT fk_price_outbox_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

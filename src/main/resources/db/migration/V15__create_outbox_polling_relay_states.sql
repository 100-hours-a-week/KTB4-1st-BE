CREATE TABLE outbox_polling_relay_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_polling_relay_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

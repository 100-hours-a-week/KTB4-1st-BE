CREATE TABLE item_price_estimation_inbox_events (
    event_id VARCHAR(36) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_view_interest_events (
    event_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    viewed_at DATETIME(6) NOT NULL,
    topic_partition INT NOT NULL,
    topic_offset BIGINT NOT NULL,
    UNIQUE KEY uk_item_view_interest_kafka_position (topic_partition, topic_offset),
    KEY idx_item_view_interest_item_time (item_id, viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_view_interest_notified_users (
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE notifications MODIFY exchange_request_id BIGINT NULL;

CREATE TABLE notifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_identity VARCHAR(36) NOT NULL,
    exchange_request_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    notification_text VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT uk_notifications_recipient_event_request
        UNIQUE (recipient_user_id, event_identity, exchange_request_id),
    CONSTRAINT fk_notifications_recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_notifications_item
        FOREIGN KEY (item_id) REFERENCES items (item_id),
    KEY idx_notifications_recipient_created_id (recipient_user_id, created_at, notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

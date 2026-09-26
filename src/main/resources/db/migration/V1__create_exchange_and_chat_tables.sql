CREATE TABLE IF NOT EXISTS exchange_requests (
    exchange_request_id BIGINT NOT NULL AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    requested_quantity INT NOT NULL,
    requested_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (exchange_request_id),
    KEY ix_exchange_request_requester_item_status (requester_id, item_id, requested_status),
    CONSTRAINT fk_exchange_request_requester FOREIGN KEY (requester_id) REFERENCES users (user_id),
    CONSTRAINT fk_exchange_request_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exchange_request_offered_items (
    exchange_request_offered_item_id BIGINT NOT NULL AUTO_INCREMENT,
    exchange_request_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (exchange_request_offered_item_id),
    UNIQUE KEY uk_exchange_offered_request_item (exchange_request_id, item_id),
    KEY ix_exchange_offered_item (item_id),
    CONSTRAINT fk_exchange_offered_request FOREIGN KEY (exchange_request_id)
        REFERENCES exchange_requests (exchange_request_id),
    CONSTRAINT fk_exchange_offered_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_rooms (
    chat_room_id BIGINT NOT NULL AUTO_INCREMENT,
    exchange_request_id BIGINT NOT NULL,
    chat_room_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    last_message_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (chat_room_id),
    UNIQUE KEY uk_chat_room_exchange_request (exchange_request_id),
    CONSTRAINT fk_chat_room_exchange_request FOREIGN KEY (exchange_request_id)
        REFERENCES exchange_requests (exchange_request_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_members (
    chat_members_id BIGINT NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    left_at DATETIME(6) NULL,
    PRIMARY KEY (chat_members_id),
    CONSTRAINT fk_chat_member_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (chat_room_id),
    CONSTRAINT fk_chat_member_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    PRIMARY KEY (message_id),
    KEY ix_chat_message_room_created (chat_room_id, created_at),
    CONSTRAINT fk_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (chat_room_id),
    CONSTRAINT fk_chat_message_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

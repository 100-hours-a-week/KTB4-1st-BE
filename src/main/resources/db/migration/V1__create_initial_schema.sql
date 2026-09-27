CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    profile_image_url VARCHAR(100) NULL,
    nickname VARCHAR(200) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    user_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE items (
    item_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    item_state VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    min_unit_price BIGINT NOT NULL,
    max_unit_price BIGINT NOT NULL,
    unit_price BIGINT NOT NULL DEFAULT 0,
    exchange_urgency_score DECIMAL(3, 2) NOT NULL,
    value_gap_tolerance_score DECIMAL(3, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (item_id),
    CONSTRAINT fk_items_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `groups` (
    group_id BIGINT NOT NULL AUTO_INCREMENT,
    group_name VARCHAR(30) NOT NULL,
    active_group_name VARCHAR(30) NULL,
    road_address VARCHAR(100) NOT NULL,
    group_longitude DECIMAL(9, 6) NOT NULL,
    group_latitude DECIMAL(9, 6) NOT NULL,
    group_content VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (group_id),
    UNIQUE KEY uk_groups_active_group_name (active_group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE social_accounts (
    social_account_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    last_login_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (social_account_id),
    CONSTRAINT uk_social_account_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
    refresh_token_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (refresh_token_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_preferences (
    user_preference_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_preference_id),
    CONSTRAINT uk_user_preference_user UNIQUE (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_preference_answers (
    user_preference_id BIGINT NOT NULL,
    answer_order INT NOT NULL,
    question VARCHAR(1000) NOT NULL,
    answer VARCHAR(1000) NOT NULL,
    PRIMARY KEY (user_preference_id, answer_order),
    CONSTRAINT ck_user_preference_question CHECK (
        question IN ('CONVERSATION_STYLE', 'DESCRIPTION_STYLE', 'OPINION_STYLE')
    ),
    CONSTRAINT ck_user_preference_answer CHECK (
        answer IN ('CONCISE', 'COMFORTABLE', 'WARM', 'BRIEF', 'MODERATE', 'DETAILED', 'CLEAR', 'NATURAL', 'INDIRECT')
    ),
    CONSTRAINT fk_user_preference_answers_preference
        FOREIGN KEY (user_preference_id) REFERENCES user_preferences (user_preference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE group_members (
    group_members_id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    location_verified_at DATETIME(6) NULL,
    user_status VARCHAR(20) NOT NULL,
    left_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (group_members_id),
    CONSTRAINT uk_group_member_group_user UNIQUE (group_id, user_id),
    KEY idx_group_member_user_status_id (user_id, user_status, group_members_id),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES `groups` (group_id),
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE group_items (
    group_item_id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (group_item_id),
    CONSTRAINT uk_group_item_group_item UNIQUE (group_id, item_id),
    KEY idx_group_item_group_deleted_item (group_id, deleted_at, item_id),
    CONSTRAINT fk_group_items_group FOREIGN KEY (group_id) REFERENCES `groups` (group_id),
    CONSTRAINT fk_group_items_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE images (
    image_id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    item_id BIGINT NULL,
    report_id BIGINT NULL,
    inquiry_id BIGINT NULL,
    object_key VARCHAR(512) NULL,
    image_url TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (image_id),
    UNIQUE KEY uk_images_object_key (object_key),
    CONSTRAINT fk_images_owner FOREIGN KEY (owner_id) REFERENCES users (user_id),
    CONSTRAINT fk_images_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE moderation_checks (
    moderation_check_id BIGINT NOT NULL AUTO_INCREMENT,
    check_id_hash CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    keyword VARCHAR(255) NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (moderation_check_id),
    UNIQUE KEY uk_moderation_check_check_id_hash (check_id_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE oauth_states (
    oauth_state_id BIGINT NOT NULL AUTO_INCREMENT,
    state_hash CHAR(64) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (oauth_state_id),
    UNIQUE KEY uk_oauth_states_state_hash (state_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_cash (
    item_cash_id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(255) NOT NULL,
    unit_price BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (item_cash_id),
    CONSTRAINT uk_item_cash_keyword UNIQUE (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_stats (
    item_id BIGINT NOT NULL,
    view_count BIGINT NOT NULL,
    like_count BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (item_id),
    CONSTRAINT fk_item_stats_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_likes (
    item_like_id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (item_like_id),
    CONSTRAINT fk_item_likes_item FOREIGN KEY (item_id) REFERENCES items (item_id),
    CONSTRAINT fk_item_likes_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_views (
    item_view_id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_counted_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (item_view_id),
    CONSTRAINT uk_item_view_item_user UNIQUE (item_id, user_id),
    CONSTRAINT fk_item_views_item FOREIGN KEY (item_id) REFERENCES items (item_id),
    CONSTRAINT fk_item_views_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exchange_requests (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exchange_request_offered_items (
    exchange_request_offered_item_id BIGINT NOT NULL AUTO_INCREMENT,
    exchange_request_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (exchange_request_offered_item_id),
    CONSTRAINT uk_exchange_offered_request_item UNIQUE (exchange_request_id, item_id),
    KEY ix_exchange_offered_item (item_id),
    CONSTRAINT fk_exchange_offered_request
        FOREIGN KEY (exchange_request_id) REFERENCES exchange_requests (exchange_request_id),
    CONSTRAINT fk_exchange_offered_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_rooms (
    chat_room_id BIGINT NOT NULL AUTO_INCREMENT,
    exchange_request_id BIGINT NOT NULL,
    chat_room_status VARCHAR(20) NOT NULL,
    last_message_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (chat_room_id),
    CONSTRAINT uk_chat_room_exchange_request UNIQUE (exchange_request_id),
    CONSTRAINT fk_chat_room_exchange_request
        FOREIGN KEY (exchange_request_id) REFERENCES exchange_requests (exchange_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_members (
    chat_members_id BIGINT NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    left_at DATETIME(6) NULL,
    PRIMARY KEY (chat_members_id),
    CONSTRAINT fk_chat_member_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (chat_room_id),
    CONSTRAINT fk_chat_member_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_messages (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (message_id),
    KEY ix_chat_message_room_created (chat_room_id, created_at),
    CONSTRAINT fk_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (chat_room_id),
    CONSTRAINT fk_chat_message_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE search_histories (
    search_history_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    last_searched_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    active_keyword VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN keyword ELSE NULL END) VIRTUAL,
    PRIMARY KEY (search_history_id),
    UNIQUE KEY uk_search_history_user_active_keyword (user_id, active_keyword),
    KEY idx_search_history_user_deleted_last_searched
        (user_id, deleted_at, last_searched_at, search_history_id),
    CONSTRAINT fk_search_histories_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

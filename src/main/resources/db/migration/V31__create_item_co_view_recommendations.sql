CREATE TABLE item_co_view_user_items (
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    last_viewed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, item_id),
    KEY idx_item_co_view_user_time_item (user_id, last_viewed_at, item_id),
    KEY idx_item_co_view_time_user_item (last_viewed_at, user_id, item_id),
    CONSTRAINT fk_item_co_view_user_items_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_item_co_view_user_items_item FOREIGN KEY (item_id) REFERENCES items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_co_view_scores (
    item_id BIGINT NOT NULL,
    related_item_id BIGINT NOT NULL,
    co_viewer_count BIGINT NOT NULL,
    calculated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (item_id, related_item_id),
    KEY idx_item_co_view_score_rank (item_id, co_viewer_count, related_item_id),
    CONSTRAINT fk_item_co_view_scores_item FOREIGN KEY (item_id) REFERENCES items (item_id),
    CONSTRAINT fk_item_co_view_scores_related_item FOREIGN KEY (related_item_id) REFERENCES items (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
